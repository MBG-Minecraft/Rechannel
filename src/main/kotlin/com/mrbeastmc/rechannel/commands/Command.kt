package com.mrbeastmc.rechannel.commands

import com.mrbeastmc.rechannel.Configuration
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

interface Command {
    companion object {
        lateinit var configuration: Configuration
    }

    fun getCommandData(): SlashCommandData

    fun execute(event: SlashCommandInteractionEvent)
}
