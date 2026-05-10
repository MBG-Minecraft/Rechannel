package com.mrbeastmc.rechannel.audio

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object RelayMixer {

	private val latestFrames = ConcurrentHashMap<Long, ByteArray>()
	private val lastUpdate = ConcurrentHashMap<Long, Long>()
	private var relayServer: AudioRelayServer? = null
	private val started = AtomicBoolean(false)
	private val staleTimeout = 80L
	private val frameSize = 3840

	fun initialize(server: AudioRelayServer) {
		relayServer = server
		if (!started.getAndSet(true)) {
			startMixLoop()
		}
	}

	private fun startMixLoop() {
		val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
			Thread(r, "relay-mixer").apply { isDaemon = true }
		}
		scheduler.scheduleAtFixedRate(::mixAndBroadcast, 0, 20, TimeUnit.MILLISECONDS)
		println("[RelayMixer] Started")
	}

	private fun mixAndBroadcast() {
		val now = System.currentTimeMillis()
		lastUpdate.entries.removeIf { now - it.value > staleTimeout }
		latestFrames.keys.removeIf { !lastUpdate.containsKey(it) }

		val frames = latestFrames.values.toList()
		val mixed = if (frames.isEmpty()) ByteArray(frameSize) else mixFrames(frames)
		relayServer?.broadcast(mixed)
	}

	fun addFrame(userId: Long, pcmData: ByteArray) {
		if (relayServer != null) {
			latestFrames[userId] = pcmData
			lastUpdate[userId] = System.currentTimeMillis()
		}
	}

	private fun mixFrames(frames: List<ByteArray>): ByteArray {
		if (frames.isEmpty()) return ByteArray(frameSize)
		if (frames.size == 1) return frames[0].copyOf()

		val count = frames.size
		val sums = IntArray(frameSize / 2)

		for (frame in frames) {
			val len = minOf(frame.size, frameSize)
			for (i in 0 until len / 2) {
				val sample = (frame[i * 2].toInt() shl 8) or (frame[i * 2 + 1].toInt() and 0xFF)
				sums[i] += sample
			}
		}

		val result = ByteArray(frameSize)
		for (i in sums.indices) {
			val s = sums[i] / count
			result[i * 2] = (s shr 8).toByte()
			result[i * 2 + 1] = (s and 0xFF).toByte()
		}
		return result
	}
}
