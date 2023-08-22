package tools

import kotlinx.serialization.json.Json
import net.server.Server
import tools.ServerJSON.path
import tools.ServerJSON.settings
import tools.ServerJSON.settingsJsonText
import tools.settings.Settings
import java.io.File
import kotlin.system.exitProcess

/**
 * ServerJSON json file loader
 *
 * Load settings.json provided from environment variable "configPath" or arguments
 *
 * @property settings Object of load and parsed settings.
 * @property settingsJsonText String of settings json file.
 * @property path Path of settings.json. If not defined in environment variables, try to load settings.json in the current directory.
 * @constructor Load and parse settings.json into Object. If failed, fill with default values.
 * @author A. S. Choe
 * @since 1.0.0
 */
object ServerJSON {
    //override val logger = logger()
    var settings: Settings
    var settingsJsonText: String
    var path: String = System.getProperty("configPath", "./settings.json")

    init {
        try {
            val file = File(path)
            settingsJsonText = file.readText()
            settings = Json.decodeFromString<Settings>(settingsJsonText)
        } catch (e: Exception) {
            println("Failed to load settings.json.")
            e.printStackTrace()
            //logger.error(e) { "Failed to load settings.json." }
            exitProcess(1)
        }
    }

    /**
     * Reload a settings.json from the disk.
     *
     * @return Is Success
     */
    suspend fun reload(): Boolean {
        try {
            settingsJsonText = File(path).readText()
            settings = Json.decodeFromString<Settings>(settingsJsonText)
            Server.worlds.forEach {
                it.reload()
            }
            return true
        } catch (e: Exception) {
            println("Failed to reload settings.json.")
            e.printStackTrace()
        }
        return false
    }

    private fun printErrorAndExit() {
        println("Failed to load settings.json.")
        //logger.error { "Failed to load settings.json." }
        exitProcess(1)
    }
}