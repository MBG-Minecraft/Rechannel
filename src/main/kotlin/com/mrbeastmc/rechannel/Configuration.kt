package com.mrbeastmc.rechannel

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class Configuration(configReset: Boolean = false) {

    private var lastModified: Long = -1
    private var configData: JsonObject
    private val config = File("configurations", "config.json")
    private val gson = Gson()

    init {
        File("configurations").apply { if (!exists()) mkdir() }
        if (!config.exists() || configReset) {
            if (configReset) println("Resetting config file from command line argument")
            this::class.java.getResourceAsStream("/config.json")!!.use { resource ->
                Files.copy(resource, config.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
        configData = readConfig()
    }

    fun isDebug(): Boolean = getRawJsonConfig().get("debug")?.asBoolean == true || Application.debug

    fun getAdministrators(): List<Long> = getRawJsonConfig().getAsJsonArray("admins")
        .map { it.asJsonObject.getAsJsonPrimitive("id").asLong }

    internal fun getRawJsonConfig(): JsonObject {
        if (config.lastModified() != lastModified) {
            configData = readConfig()
            lastModified = config.lastModified()
        }
        return configData
    }

    private fun readConfig(): JsonObject = gson.fromJson(config.readText(), JsonObject::class.java)

}
