package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.audio.RelayAudioSendHandler
import com.mrbeastmc.rechannel.commands.Command.Companion.configuration
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.concurrent.TimeUnit

class RelayListen : ListenerAdapter(), Command {

	override fun getCommandData(): SlashCommandData =
		Commands.slash("relay-listen", "Connects to a relay server and plays audio in the current voice channel")
			.addOption(OptionType.STRING, "host", "The relay server hostname or IP. Default is 127.0.0.1", false)
			.addOption(OptionType.INTEGER, "port", "The relay server port", false)
			.setDefaultPermissions(DefaultMemberPermissions.DISABLED)

	override fun execute(event: SlashCommandInteractionEvent) {
		val member = event.member ?: return
		if (configuration.getAdministrators().find { it == member.idLong } == null) {
			event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
			return
		}

		val guild = event.guild ?: return
		val audioManager = guild.audioManager

		if (!audioManager.isConnected) {
			event.reply("I need to be in a voice channel first. Use /record to join one.").setEphemeral(true).queue()
			return
		}

		val guildId = guild.idLong
		if (activeHandlers.containsKey(guildId)) {
			event.reply("Already relaying audio. Use /relay-stop first.").setEphemeral(true).queue()
			return
		}

		val host = event.getOption("host")?.asString ?: "127.0.0.1"
		val port = event.getOption("port")?.asInt ?: 1000

		val channel = audioManager.connectedChannel
		audioManager.isSelfMuted = false
		audioManager.sendingHandler = null
		audioManager.closeAudioConnection()

		event.deferReply(true).queue { hook ->
			Thread {
				var attempts = 0
				while (guild.audioManager.isConnected && attempts < 30) {
					Thread.sleep(100)
					attempts++
				}

				if (channel != null) {
					guild.audioManager.openAudioConnection(channel)
				}

				val handler = RelayAudioSendHandler(host, port)
				handler.connect()

				if (!handler.isConnected()) {
					if (guild.audioManager.isConnected) {
						audioManager.isSelfMuted = true
					}
					hook.editOriginal("Failed to connect to relay server at $host:$port.").queue()
					return@Thread
				}

				if (!handler.waitForJitterBuffer()) {
					handler.disconnect()
					if (guild.audioManager.isConnected) {
						audioManager.isSelfMuted = true
					}
					hook.editOriginal("Timed out waiting for audio from relay server.").queue()
					return@Thread
				}

				audioManager.sendingHandler = handler
				activeHandlers[guildId] = handler

				hook.editOriginal("Now playing relayed audio from $host:$port.").queue()
				hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
			}.start()
		}
	}

	companion object {
		val activeHandlers = mutableMapOf<Long, RelayAudioSendHandler>()
	}
}
