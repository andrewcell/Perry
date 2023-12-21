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

    /**
     * This block of code is the initializer for the ServerJSON object.
     * It attempts to read a file specified by the path variable and parse it into a Settings object.
     * The path variable is set to the value of the system property "configPath", or "./settings.json" if "configPath" is not defined.
     * The parsed Settings object and the raw text of the file are stored in the settings and settingsJsonText variables respectively.
     *
     * If the file cannot be read or parsed, an error message is printed to the console, the stack trace of the exception is printed,
     * and the program is terminated with a non-zero exit code.
     *
     * @throws Exception if the file specified by path cannot be read or parsed into a Settings object.
     */
    init {
        try {
            val file = File(path) // Attempt to open the file specified by path
            settingsJsonText = file.readText() // Read the text of the file
            settings = Json.decodeFromString<Settings>(settingsJsonText) // Parse the text into a Settings object
        } catch (e: Exception) { // If an exception is thrown
            println("Failed to load settings.json.") // Print an error message
            e.printStackTrace() // Print the stack trace of the exception
            exitProcess(1) // Terminate the program with a non-zero exit code
        }
    }

    /**
     * This function is responsible for reloading the settings.json file from the disk.
     * It first reads the file specified by the path variable and parses it into a Settings object.
     * The parsed Settings object and the raw text of the file are stored in the settings and settingsJsonText variables respectively.
     * It then iterates over all the worlds in the Server and calls the reload method on each of them.
     * If the file is successfully read and parsed, and all worlds are successfully reloaded, the function returns true.
     * If any exception is thrown during this process, an error message is printed to the console, the stack trace of the exception is printed,
     * and the function returns false.
     *
     * @return Boolean indicating the success of the operation. Returns true if the settings.json file is successfully reloaded and all worlds are reloaded. Returns false otherwise.
     * @throws Exception if the file specified by path cannot be read or parsed into a Settings object, or if any world cannot be reloaded.
     */
    suspend fun reload(): Boolean {
        try {
            settingsJsonText = File(path).readText() // Read the text of the file
            settings = Json.decodeFromString<Settings>(settingsJsonText) // Parse the text into a Settings object
            Server.worlds.forEach {
                it.reload() // Reload each world in the Server
            }
            return true // Return true if the file is successfully read and parsed, and all worlds are reloaded
        } catch (e: Exception) { // If an exception is thrown
            println("Failed to reload settings.json.") // Print an error message
            e.printStackTrace() // Print the stack trace of the exception
            return false // Return false if any exception is thrown
        }
    }

    private fun printErrorAndExit() {
        println("Failed to load settings.json.")
        //logger.error { "Failed to load settings.json." }
        exitProcess(1)
    }
}