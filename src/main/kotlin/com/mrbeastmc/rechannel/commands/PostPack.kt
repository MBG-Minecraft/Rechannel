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
import net.dv8tion.jda.api.utils.FileUpload
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PostPack : ListenerAdapter(), Command {

    override fun getCommandData(): SlashCommandData =
        Commands.slash("post", "Posts a mod pack embed to this channel")
            .addOption(OptionType.USER, "user", "The user to collect audio files from", true)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED)

    override fun execute(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        if (configuration.getAdministrators().find { it == member.idLong } == null) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
            return
        }
        val user = event.getOption("user")?.asUser ?: return
        val username = user.name
        val recordingsDir = File("recordings/$username/")
        if (!recordingsDir.exists() || !recordingsDir.isDirectory) {
            event.reply("No recordings found for $username.").setEphemeral(true).queue()
            return
        }
        val dates = recordingsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        if (dates.isEmpty()) {
            event.reply("No recordings found for $username.").setEphemeral(true).queue()
            return
        }
        val dateList = dates.joinToString("\n") { it }
        event.reply("Recordings for $username:\n$dateList").setEphemeral(true).queue()
    }

}
