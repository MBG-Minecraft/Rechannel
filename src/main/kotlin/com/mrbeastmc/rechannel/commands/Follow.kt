package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.commands.Command.Companion.configuration
import com.mrbeastmc.rechannel.listeners.AudioReceiveListener
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.concurrent.TimeUnit

class Follow : ListenerAdapter(), Command {

	override fun getCommandData(): SlashCommandData =
		Commands.slash("follow", "Follows a user to their voice channel")
			.addOption(OptionType.USER, "user", "The user to follow around", true)
			.addOption(OptionType.NUMBER, "seconds", "The amount of seconds to release each segment of audio by. Default is 15 minutes", false)
			.addOption(OptionType.NUMBER, "volume", "The volume to record at, must be between 0.0 and 1.0. Default is 1.0", false)
			.setDefaultPermissions(DefaultMemberPermissions.DISABLED)

	override fun execute(event: SlashCommandInteractionEvent) {
		val member = event.member ?: return
		if (configuration.getFollowing() != null) {
			val following = event.jda.getUserById(configuration.getFollowing()!!)
			event.reply("I'm already following ${following?.name ?: "someone"}.").setEphemeral(true).queue()
			return
		}
		if (configuration.getAdministrators().find { it == member.idLong } == null) {
			event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
			return
		}
		val seconds = event.getOption("seconds")?.asLong ?: TimeUnit.MINUTES.toMillis(15)
		if (seconds < 0) {
			event.reply("The provided seconds is negative.").setEphemeral(true).queue()
			return
		}
		val volume = event.getOption("volume")?.asDouble ?: 1.0
		if (volume < 0.0 || volume > 1.0) {
			event.reply("The provided volume is not between 0.0 and 1.0.").setEphemeral(true).queue()
			return
		}
		val user = event.getOption("user")?.asUser ?: return
		configuration.setFollowing(user.idLong)
		event.jda.presence.activity = Activity.of(Activity.ActivityType.LISTENING, user.effectiveName)
		event.reply("Now following ${user.asMention}.").setEphemeral(true).queue {
			it.deleteOriginal().queueAfter(5, TimeUnit.SECONDS)
		}
		val guild = event.guild ?: return
		val voiceState = guild.audioManager.connectedChannel?.members?.find { it.user.idLong == user.idLong }
		if (voiceState != null) {
			(guild.audioManager.receivingHandler as? AudioReceiveListener)?.shutdown()
		}
		Thread {
			while (configuration.getFollowing() == user.idLong) {
				val voiceState = guild.audioManager.connectedChannel?.members?.find { it.user.idLong == user.idLong }
				if (voiceState != null) {
					Thread.sleep(1000)
					continue
				}

				guild.voiceChannels.find { channel -> channel.members.any { it.user.idLong == user.idLong } }?.let { channel ->
					guild.audioManager.receivingHandler?.let { (it as? AudioReceiveListener)?.shutdown() }
					guild.audioManager.openAudioConnection(channel)
					guild.audioManager.receivingHandler = AudioReceiveListener(saveTimeMilliseconds = seconds, volume)
					guild.audioManager.isSelfMuted = true
				}
				Thread.sleep(1000)
			}
		}.start()
	}

}
