package com.mrbeastmc.rechannel.audio

import net.dv8tion.jda.api.audio.AudioSendHandler
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class RelayAudioSendHandler(private val host: String, private val port: Int) : AudioSendHandler {

	private val queue = ConcurrentLinkedQueue<ByteArray>()
	private val connected = AtomicBoolean(false)
	private var nextFrame: ByteArray? = null
	private var socket: Socket? = null
	private var reader: Thread? = null

	@Volatile
	var gain: Double = 1.0

	private val primed = AtomicBoolean(false)
	private val jitterFrames = 3

	fun connect() {
		try {
			socket = Socket(host, port).also { it.tcpNoDelay = true }
			connected.set(true)
			println("[RelayClient] Connected to $host:$port")

			reader = Thread({ readLoop() }, "relay-client-reader").apply { isDaemon = true }
			reader!!.start()
		} catch (e: Exception) {
			println("[RelayClient] Failed to connect to $host:$port: ${e.message}")
		}
	}

	fun waitForJitterBuffer(timeoutMs: Long = 3000): Boolean {
		val deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline && connected.get()) {
			if (queue.size >= jitterFrames) {
				primed.set(true)
				return true
			}
			Thread.sleep(5)
		}
        
		if (queue.size >= jitterFrames) {
			primed.set(true)
			return true
		}
		return false
	}

	fun resetPrime() {
		primed.set(false)
	}

	private fun readLoop() {
		try {
			val input = DataInputStream(BufferedInputStream(socket!!.getInputStream()))
			while (connected.get()) {
				val length = input.readInt()
				if (length <= 0 || length > 65536) {
					println("[RelayClient] Invalid frame length: $length, disconnecting")
					break
				}
				val data = ByteArray(length)
				input.readFully(data)
				queue.offer(data)
			}
		} catch (e: Exception) {
			if (connected.get()) println("[RelayClient] Connection lost: ${e.message}")
		} finally {
			connected.set(false)
			try {
				socket?.close()
			} catch (_: Exception) {
			}
			queue.clear()
			nextFrame = null
		}
	}

	override fun canProvide(): Boolean {
		if (nextFrame != null) return true
		if (!primed.get()) return false
		nextFrame = queue.poll()
		return nextFrame != null
	}

	override fun provide20MsAudio(): ByteBuffer {
		val frame = nextFrame ?: ByteArray(3840)
		nextFrame = null
		if (gain != 1.0) applyGain(frame)
		return ByteBuffer.wrap(frame)
	}

	private fun applyGain(data: ByteArray) {
		for (i in data.indices step 2) {
			val sample = (data[i].toInt() shl 8) or (data[i + 1].toInt() and 0xFF)
			val amplified = (sample * gain).toInt().coerceIn(-32768, 32767)
			data[i] = (amplified shr 8).toByte()
			data[i + 1] = (amplified and 0xFF).toByte()
		}
	}

	override fun isOpus(): Boolean = false

	fun disconnect() {
		connected.set(false)
		try {
			socket?.close()
		} catch (_: Exception) {
		}
		reader?.interrupt()
		queue.clear()
		nextFrame = null
		println("[RelayClient] Disconnected")
	}

	fun isConnected(): Boolean = connected.get()
}
