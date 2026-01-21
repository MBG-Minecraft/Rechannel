package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.commands.Command.Companion.configuration
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.awt.Color
import java.util.concurrent.TimeUnit

class PostPack : ListenerAdapter(), Command {

    override fun getCommandData(): SlashCommandData =
        Commands.slash("post", "Posts a mod pack embed to this channel")
            .addOption(OptionType.STRING, "title", "The title of the event", true)
            .addOption(OptionType.STRING, "description", "The description of the event", true)
            .addOption(OptionType.STRING, "url", "The url to the mod pack of the event", true)
            .addOption(OptionType.STRING, "color", "The hex color of the embed", false)
            .addOption(OptionType.ROLE, "mention", "An optional role to mention", false)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED)

    override fun execute(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        if (configuration.getAdministrators().find { it == member.idLong } == null) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
            return
        }
        val color = (event.getOption("color")?.asString ?: "#08B0D5").let {
            try {
                Color.decode(it)
            } catch (e: NumberFormatException) {
                event.reply("Invalid color format. Please use a valid hex color code.").setEphemeral(true).queue()
                return
            }
        }
        val title = event.getOption("title")!!.asString
        val url = event.getOption("url")!!.asString
        val embed = EmbedBuilder()
            .setTitle("Welcome to $title event!")
            .addField("Description", event.getOption("description")!!.asString, false)
            .addField("Installation",
                "Once you have <https://modrinth.com/app> installed,\n" +
                "you can download the mod pack at <$url>\n" +
                "and install it by opening the `.mrpack`\n\n" +
                "Once you have the mod pack installed, we recommend you to increase the allocated ram to 5GB+ in the pack settings", false)
            .setImage("https://i.mrbeastgaming.dev/ram.png")
            .setUrl(url)
            .setColor(color)
            .build()
        val textChannel = event.channel.asTextChannel()
        textChannel.sendMessageEmbeds(embed).queue()
        event.getOption("mention")?.asRole?.let { role ->
            textChannel.sendMessage(role.asMention).queue()
        }
        event.reply("Mod pack posted successfully!").setEphemeral(true).queue { it.deleteOriginal().queueAfter(5, TimeUnit.SECONDS) }
    }

}
