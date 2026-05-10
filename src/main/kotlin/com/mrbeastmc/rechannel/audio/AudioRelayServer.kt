package com.mrbeastmc.rechannel.audio

import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class AudioRelayServer private constructor(private val port: Int) {

	private val serverSocket: ServerSocket
	private val clients = CopyOnWriteArrayList<ClientConnection>()
	private val running = AtomicBoolean(true)
	private val broadcastBuffer = ByteArray(4 + 3840)

	init {
		val socket = ServerSocket()
		socket.reuseAddress = true
		socket.bind(InetSocketAddress(port))
		serverSocket = socket
	}

	companion object {
		private val instance = AtomicReference<AudioRelayServer?>()

		fun start(port: Int): AudioRelayServer? {
			synchronized(instance) {
				if (instance.get() != null) return instance.get()
				return try {
					AudioRelayServer(port).also { it.startAccepting() }.also { instance.set(it) }
				} catch (e: IOException) {
					System.err.println("[RelayServer] Failed to start on port $port: ${e.message}")
					null
				}
			}
		}

		fun getInstance(): AudioRelayServer? = instance.get()

		fun shutdown() {
			synchronized(instance) {
				instance.getAndSet(null)?.stop()
			}
		}
	}

	private fun startAccepting() {
		Thread({ acceptLoop() }, "relay-accept").apply { isDaemon = true }.start()
		println("[RelayServer] Listening on port $port")
	}

	private fun acceptLoop() {
		try {
			while (running.get()) {
				val socket = serverSocket.accept()
				socket.tcpNoDelay = true
				socket.soTimeout = 10000
				clients.add(ClientConnection(socket))
				println("[RelayServer] Client connected from ${socket.inetAddress.hostAddress}")
			}
		} catch (e: SocketException) {
			if (running.get()) e.printStackTrace()
		} catch (e: IOException) {
			if (running.get()) e.printStackTrace()
		}
	}

	fun broadcast(data: ByteArray) {
		if (clients.isEmpty()) return
		val size = data.size
		broadcastBuffer[0] = (size shr 24).toByte()
		broadcastBuffer[1] = (size shr 16).toByte()
		broadcastBuffer[2] = (size shr 8).toByte()
		broadcastBuffer[3] = size.toByte()
		data.copyInto(broadcastBuffer, 4, 0, size)
		clients.removeIf { !it.send(broadcastBuffer, 4 + size) }
	}

	fun stop() {
		running.set(false)
		try {
			serverSocket.close()
		} catch (_: Exception) {
		}
		clients.forEach { it.close() }
		clients.clear()
	}

	private class ClientConnection(private val socket: Socket) {
		private val out: OutputStream = socket.getOutputStream()

		fun send(buffer: ByteArray, len: Int): Boolean {
			return try {
				synchronized(out) {
					out.write(buffer, 0, len)
					out.flush()
				}
				true
			} catch (e: Exception) {
				close()
				false
			}
		}

		fun close() {
			try {
				socket.close()
			} catch (_: Exception) {
			}
		}
	}
}
