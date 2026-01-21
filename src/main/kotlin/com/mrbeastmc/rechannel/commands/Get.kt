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
import kotlin.jvm.java

class Get : ListenerAdapter(), Command {

    override fun getCommandData(): SlashCommandData =
        Commands.slash("get", "Collect an audio recording from a date for a user")
            .addOption(OptionType.USER, "user", "The user to collect audio files from", true)
            .addOption(OptionType.STRING, "date", "The date in the format yyyy-MM-dd", true)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED)

    override fun execute(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        if (configuration.getAdministrators().find { it == member.idLong } == null) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
            return
        }
        val date = event.getOption("date")?.asString ?: return
        val user = event.getOption("user")?.asUser ?: return
        val username = user.name
        val recordingsDir = File("recordings/$username/$date/")
        if (!recordingsDir.exists() || !recordingsDir.isDirectory) {
            event.reply("No recordings found for $username on $date.").setEphemeral(true).queue()
            return
        }

        val zipFile = File("recordings/$username/$date.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
            zipOut.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)
            recordingsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                zipOut.putNextEntry(ZipEntry(file.relativeTo(recordingsDir).path))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
        val fileUpload = FileUpload.fromData(zipFile, "$username-$date.zip")
        event.replyFiles(fileUpload).setEphemeral(true).queue { zipFile.delete() }
    }

}
