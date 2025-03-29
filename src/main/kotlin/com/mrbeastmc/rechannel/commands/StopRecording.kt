package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.commands.Command.Companion.configuration
import com.mrbeastmc.rechannel.listeners.AudioReceiveListener
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class StopRecording : ListenerAdapter(), Command {

    override fun getCommandData(): SlashCommandData =
        Commands.slash("stoprecording", "Stops listening and recording in the connected voice channel")
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED)

    override fun execute(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        if (configuration.getAdministrators().find { it == member.idLong } == null) {
            event.reply("You do not have permission to use this command").setEphemeral(true).queue()
            return
        }
        (event.guild?.audioManager?.receivingHandler as? AudioReceiveListener)?.shutdown()
        event.guild?.audioManager?.closeAudioConnection()?.let {
            event.reply("Stopped recording").setEphemeral(true).queue()
        } ?: run {
            event.reply("Wasn't recording in this guild").setEphemeral(true).queue()
        }
    }

}
