package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.commands.Command.Companion.configuration
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.concurrent.TimeUnit

class RelayVolume : ListenerAdapter(), Command {

	override fun getCommandData(): SlashCommandData =
		Commands.slash("relay-volume", "Sets the gain of the relayed audio (default 1.0)")
			.addOption(OptionType.NUMBER, "gain", "Volume multiplier (0.0 = silence, 1.0 = normal, 2.0 = double)", true)
			.setDefaultPermissions(DefaultMemberPermissions.DISABLED)

	override fun execute(event: SlashCommandInteractionEvent) {
		val member = event.member ?: return
		if (configuration.getAdministrators().find { it == member.idLong } == null) {
			event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
			return
		}

		val guild = event.guild ?: return
		val handler = RelayListen.activeHandlers[guild.idLong] ?: run {
			event.reply("Not in relay-listen mode. Use /relay-listen first.").setEphemeral(true).queue()
			return
		}

		val gain = event.getOption("gain")!!.asDouble
		if (gain < 0.0 || gain > 10.0) {
			event.reply("Gain must be between 0.0 and 10.0.").setEphemeral(true).queue()
			return
		}

		handler.gain = gain
		event.reply("Relay gain set to $gain.").setEphemeral(true).queue {
			it.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
		}
	}
}
