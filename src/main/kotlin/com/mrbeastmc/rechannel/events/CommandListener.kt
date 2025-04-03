package com.mrbeastmc.rechannel.events

import com.mrbeastmc.rechannel.commands.Command
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.reflections.Reflections

class CommandListener : ListenerAdapter() {

    private val commands = mutableMapOf<Command, SlashCommandData>()
    private val eventListeners = mutableListOf<EventListener>()

    init {
        try {
            val commandClasses = Reflections("com.mrbeastmc.rechannel.commands").getSubTypesOf(Command::class.java)
            commandClasses.forEach { clazz ->
                val command = clazz.getDeclaredConstructor().newInstance()
                commands[command] = command.getCommandData()
                if (command is EventListener) {
                    eventListeners.add(command)
                }
            }
        } catch (e: Exception) {
            System.err.println("Error at loading the commands: ${e.message}")
        }
    }

    fun getEventListeners(): Collection<EventListener> = eventListeners.toList()
    fun getCommands(): Collection<SlashCommandData> = commands.values.toList()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        commands.filterValues { it.name == event.name }.keys.firstOrNull()?.execute(event)
    }

}
