package com.mrbeastmc.rechannel

import com.mrbeastmc.rechannel.commands.Command
import com.mrbeastmc.rechannel.events.CommandListener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import java.io.File

class Application {

    companion object {

        internal lateinit var instance: JDA
        internal var debug: Boolean = false

        @JvmStatic
        fun main(args: Array<String>) {
            val commandListener = CommandListener()
            debug = args.any { it.equals("--debug", ignoreCase = true) == true }
            val configuration = Configuration(args.any { it.equals("--resetConfig", ignoreCase = true) == true })
            Command.configuration = configuration
            val cachePolicy = MemberCachePolicy.ALL.and(MemberCachePolicy.lru(100_000))
            val intents = mutableListOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES)
            val token: String = System.getProperty("DISCORD_TOKEN")
                ?: System.getenv("DISCORD_TOKEN")
                ?: loadTokenFromEnvFile()
                ?: throw IllegalStateException("No token found. Please set the DISCORD_TOKEN environment variable or create a .env file with the token.")


            instance = JDABuilder.createDefault(token, intents)
                // Done like this because JDA doesn't like Kotlin collectors or arrays.
                .apply { commandListener.getEventListeners().forEach { addEventListeners(it) } }
                .addEventListeners(commandListener)
                .setMemberCachePolicy(cachePolicy)
                .build()
                .awaitReady()

            instance.updateCommands().addCommands(commandListener.getCommands()).queue()
            instance.guilds.forEach { it.audioManager.closeAudioConnection() }
        }

        fun loadTokenFromEnvFile(): String? {
            File(".env").forEachLine { line ->
                val (key, value) = line.split("=").map { it.trim() }
                if (key.isNotEmpty() && !key.startsWith("#")) {
                    System.setProperty(key, value)
                }
            }
            return System.getProperty("DISCORD_TOKEN")
        }
    }

}
