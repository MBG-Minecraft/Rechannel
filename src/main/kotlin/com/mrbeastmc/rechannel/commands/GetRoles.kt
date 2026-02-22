package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.commands.Command.Companion.configuration
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.utils.FileUpload

class GetRoles : ListenerAdapter(), Command {

    override fun getCommandData(): SlashCommandData =
        Commands.slash("getroles", "Collect all users with a specific role")
            .addOption(OptionType.ROLE, "role", "The role to scan for players", true)
            .addOption(OptionType.BOOLEAN, "public", "Whether the response should be public or not", false)
            .setDefaultPermissions(DefaultMemberPermissions.DISABLED)

    override fun execute(event: SlashCommandInteractionEvent) {
        val member = event.member ?: return
        if (configuration.getAdministrators().find { it == member.idLong } == null) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue()
            return
        }

        val ephemeral = event.getOption("public")?.asBoolean?.not() ?: true
        val role = event.getOption("role")?.asRole ?: return
        val action = event.deferReply(ephemeral)
        val guild = event.guild ?: return

        val fileName = "role_${role.id}_guild_${guild.idLong}.txt"
        guild.findMembersWithRoles(role)
            .onSuccess { membersWithRole ->
                val builder = StringBuilder()
                builder.append("Users with role: ").append(role.name).append(" (").append(role.id).append(")\n")
                builder.append("Count: ").append(membersWithRole.size).append("\n\n")
                for (m in membersWithRole.sortedBy { it.user.name }) {
                    builder.append(m.user.name)
                        .append(" (").append(m.user.id).append(")\n")
                }
                action.addFiles(FileUpload.fromData(builder.toString().toByteArray(Charsets.UTF_8), fileName))
                action.queue()
            }
            .onError { error ->
                action.setContent("Failed to fetch members, try again: ${error.message}").queue()
            }
    }

}
