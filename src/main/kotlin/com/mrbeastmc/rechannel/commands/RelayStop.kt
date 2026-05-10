package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.commands.Command.Companion.configuration
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.concurrent.TimeUnit

class RelayStop : ListenerAdapter(), Command {

	override fun getCommandData(): SlashCommandData =
		Commands.slash("relay-stop", "Stops playing relayed audio and disconnects from the relay server")
			.setDefaultPermissions(DefaultMemberPermissions.DISABLED)

	override fun execute(event: SlashCommandInteractionEvent) {
		val member = event.member ?: return
		if (configuration.getAdministrators().find { it == member.idLong } == null) {
			event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
			return
		}

		val guild = event.guild ?: return
		val guildId = guild.idLong
		val audioManager = guild.audioManager

		val handler = RelayListen.activeHandlers[guildId] ?: run {
			event.reply("Not in relay-listen mode.").setEphemeral(true).queue()
			return
		}

		handler.disconnect()
		audioManager.sendingHandler = null
		audioManager.isSelfMuted = true
		RelayListen.activeHandlers.remove(guildId)

		event.reply("Stopped relay listening.").setEphemeral(true).queue {
			it.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
		}
	}
}
