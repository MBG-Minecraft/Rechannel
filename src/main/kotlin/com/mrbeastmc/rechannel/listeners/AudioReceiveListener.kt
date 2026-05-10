package com.mrbeastmc.rechannel.listeners

import com.mrbeastmc.rechannel.Application
import com.mrbeastmc.rechannel.audio.RelayMixer
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.OpusPacket
import net.sourceforge.lame.lowlevel.LameEncoder
import net.sourceforge.lame.mp3.Lame
import net.sourceforge.lame.mp3.MPEGMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AudioReceiveListener(
	saveTimeMilliseconds: Long = TimeUnit.MINUTES.toMillis(2),
	private val volume: Double = 1.0
) : AudioReceiveHandler {

	private val executor = ScheduledThreadPoolExecutor(2)
	private val data = mutableListOf<RecordingData>()

	init {
		executor.scheduleAtFixedRate(
			::saveRecordings,
			saveTimeMilliseconds,
			saveTimeMilliseconds,
			TimeUnit.MILLISECONDS
		)
	}

	override fun canReceiveEncoded(): Boolean = true
	override fun handleEncodedAudio(packet: OpusPacket) {
		val userId = packet.userId
		val pcmData = try {
			packet.getAudioData(volume)
		} catch (e: Exception) {
			null
		} ?: return

		val userData = data.find { it.userId == userId } ?: RecordingData(executor, userId).also { data.add(it) }
		userData.addData(pcmData)

		RelayMixer.addFrame(userId, pcmData)
	}

	private fun saveRecordings() {
		data.forEach { it.save() }
	}

	fun shutdown() {
		executor.shutdown()
		saveRecordings()
	}

	private class RecordingData(executor: ScheduledThreadPoolExecutor, val userId: Long) {
		private val username: String = Application.instance.getUserById(userId)?.name ?: "$userId"
		private val silence = ByteArrayOutputStream()
		private val raw = ByteArrayOutputStream()

		// Value represents the amount of milliseconds to wait before writing.
		// Each packet is 20ms, so we need to wait 20ms before writing for each incoming packet.
		// Otherwise, you'll be inserting silence data into valid data.
		private var writing = 0

		init {
			executor.scheduleAtFixedRate({
				if (writing > 0) {
					writing--
					return@scheduleAtFixedRate
				}
				silence.write(ByteArray(192))
			}, 0, 1, TimeUnit.MILLISECONDS)
		}

		fun addData(data: ByteArray) {
			writing += 20 // 20ms
			this.silence.write(data)
			this.raw.write(data)
		}

		fun getSilenceDataAndClear(): ByteArray {
			val bytes = silence.toByteArray()
			silence.reset()
			return encodePcmToMp3(bytes)
		}

		fun getRawDataAndClear(): ByteArray {
			val bytes = raw.toByteArray()
			raw.reset()
			return encodePcmToMp3(bytes)
		}

		private fun encodePcmToMp3(pcm: ByteArray): ByteArray {
			val encoder = LameEncoder(AudioReceiveHandler.OUTPUT_FORMAT, 128, MPEGMode.STEREO, Lame.QUALITY_HIGHEST, false)
			val buffer = ByteArray(encoder.pcmBufferSize)
			val mp3 = ByteArrayOutputStream()

			var currentPcmPosition = 0
			while (currentPcmPosition < pcm.size) {
				val bytesToTransfer = minOf(buffer.size, pcm.size - currentPcmPosition)
				val bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer)
				currentPcmPosition += bytesToTransfer
				mp3.write(buffer, 0, bytesWritten)
			}

			encoder.close()
			return mp3.toByteArray()
		}

		fun save() {
			fun write(path: String, data: ByteArray) {
				File(path).apply {
					parentFile.mkdirs()
					writeBytes(data)
				}
			}

			CompletableFuture.runAsync {
				val day = SimpleDateFormat("yyyy-MM-dd").format(Date())
				val date = SimpleDateFormat("yyyy-MM-dd-hh.mm.ss").format(Date())
				val channel = Application.instance.selfUser.jda.guilds.firstOrNull()?.audioManager?.connectedChannel
				val name = if (channel != null) {
					"channel ${channel.name} "
				} else {
					""
				}
				write("recordings/$username/$day/$date/${name}withSilence.mp3", getSilenceDataAndClear())
				write("recordings/$username/$day/$date/${name}rawNoSilence.mp3", getRawDataAndClear())
			}.exceptionally { e ->
				e.printStackTrace()
				null
			}
		}

	}

}
