package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.commands.Command.Companion.configuration
import com.mrbeastmc.rechannel.listeners.AudioReceiveListener
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.concurrent.TimeUnit

class StartRecording : ListenerAdapter(), Command {

	override fun getCommandData(): SlashCommandData =
		Commands.slash("record", "Starts listening and recording all voice in the channel")
			.addOption(OptionType.CHANNEL, "channel", "The channel to start recording voices in", true)
			.addOption(OptionType.INTEGER, "seconds", "The amount of seconds to release each segment of audio by. Default is 15 minutes", false)
			.addOption(OptionType.NUMBER, "volume", "The volume to record at, must be between 0.0 and 1.0. Default is 1.0", false)
			.setDefaultPermissions(DefaultMemberPermissions.DISABLED)

	override fun execute(event: SlashCommandInteractionEvent) {
		val member = event.member ?: return
		if (configuration.getAdministrators().find { it == member.idLong } == null) {
			event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
			return
		}
		val channel: VoiceChannel = event.getOption("channel")!!.asChannel.takeIf { it.type.isMessage }?.asVoiceChannel() ?: run {
			event.reply("The provided channel is not a voice channel.").setEphemeral(true).queue()
			return
		}
		val self = event.jda.selfUser
		if (channel.members.map { it.idLong }.contains(self.idLong)) {
			event.reply("I'm already in this voice channel recording.").setEphemeral(true).queue()
			return
		}
		val audioManager = channel.guild.audioManager
		if (audioManager.isConnected) {
			event.reply("I'm already in a voice channel recording.").setEphemeral(true).queue()
			return
		}
		audioManager.openAudioConnection(channel)
		val seconds = event.getOption("seconds")?.asInt?.toLong() ?: TimeUnit.MINUTES.toSeconds(15)
		if (seconds < 0) {
			event.reply("The provided seconds is negative.").setEphemeral(true).queue()
			return
		}
		val volume = event.getOption("volume")?.asDouble ?: 1.0
		if (volume < 0.0 || volume > 1.0) {
			event.reply("The provided volume is not between 0.0 and 1.0.").setEphemeral(true).queue()
			return
		}
		audioManager.receivingHandler = AudioReceiveListener(saveTimeMilliseconds = TimeUnit.SECONDS.toMillis(seconds), volume)
		audioManager.isSelfMuted = true
		event.reply("Started recording in ${channel.name}.").setEphemeral(true).queue {
			it.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
		}
	}

}
