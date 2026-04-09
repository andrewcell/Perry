package scripting.map

import client.Client
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.FileReader
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptEngineManager

/**
 * Object responsible for managing map-related scripts in a game.
 *
 * This class provides functionality to load, execute, and manage scripts associated with map events.
 * The scripts are written in JavaScript and executed via a Graal.js engine. It also supports reloading
 * scripts and checking for their existence in specific directories. The scripts are invoked dynamically
 * based on specific map-related events, such as a user entering a map.
 */
object MapScriptManager {
    /**
     * Logger instance used for logging messages, errors, or other information
     * within the context of the script execution and handling processes.
     *
     * This property is used for logging purposes such as reporting errors,
     * debugging information, and status updates during the lifecycle of map
     * script handling, ensuring better traceability and maintainability.
     */
    private val logger = KotlinLogging.logger {  }
    /**
     * A mutable map storing script names mapped to their respective invocable instances.
     *
     * This property is used to manage and cache the scripts associated with map events. Each script
     * can be identified by a unique name and is executed through its corresponding `Invocable` instance.
     *
     * The map allows efficient retrieval and execution of scripts, avoiding the need to reload or
     * recompile scripts multiple times during runtime. It can be cleared or updated as needed, such as
     * during the process of reloading scripts.
     */
    private val scripts = mutableMapOf<String, Invocable>()
    /**
     * Represents the factory for creating instances of script engines capable of executing Graal.js (JavaScript)
     * code. This factory is initialized using a `ScriptEngineManager` to fetch the engine implementation for
     * Graal.js.
     *
     * This property is primarily used in the context of scripting systems, where map scripts or event scripts
     * can be dynamically loaded, compiled, and executed using the Graal.js engine. The factory provides a
     * mechanism to get the script engine, which supports script compilation and invocation of script functions.
     *
     * Typical usage includes tasks such as loading a JavaScript file, compiling the script, and invoking
     * methods defined within the script for game-related interactions or animations.
     */
    private val sef = ScriptEngineManager().getEngineByName("graal.js").factory

    /**
     * Clears the list of scripts, effectively reloading them by removing all current entries.
     *
     * This method is used to reset the state of the loaded scripts, allowing the system
     * to reinitialize or repopulate them as needed. Any further operations relying on
     * the script list will start with an empty state after this method is called.
     */
    fun reloadScripts() = scripts.clear()

    /**
     * Checks whether the specified map script file exists in the appropriate directory.
     *
     * @param scriptName The name of the script to check for existence.
     * @param firstUser A flag indicating whether the script should be in the "onFirstUserEnter" directory (true)
     * or the "onUserEnter" directory (false).
     * @return `true` if the script exists in the directory, `false` otherwise.
     */
    fun scriptExists(scriptName: String, firstUser: Boolean): Boolean {
        val user = if (firstUser) "onFirstUserEnter" else "onUserEnter"
        val scriptFile = File("scripts/map/$user/$scriptName.js")
        return scriptFile.exists()
    }

    /**
     * Executes or compiles a map script based on the provided script name and user information.
     *
     * @param c The client instance that serves as the context for the map script.
     * @param scriptName The name of the script to be executed or compiled.
     * @param firstUser A flag indicating whether the user is the first to enter the map,
     *                  determining the script type to be loaded ('onFirstUserEnter' or 'onUserEnter').
     */
    fun getMapScript(c: Client, scriptName: String, firstUser: Boolean) {
        try {
            if (scripts.containsKey(scriptName)) {
                scripts[scriptName]?.invokeFunction("start", MapScriptMethods(c))
                return
            }
            val type = if (firstUser) "onFirstUserEnter" else "onUserEnter"
            val scriptFile = File("scripts/map/$type/$scriptName.js")
            if (!scriptExists(scriptName, firstUser)) return
            val portal = sef.scriptEngine
            val fr = FileReader(scriptFile)
            val compiled = (portal as Compilable).compile(fr)
            compiled.eval()
            portal as Invocable
            scripts[scriptName] = portal
            portal.invokeFunction("start", MapScriptMethods(c))
        } catch (e: Exception) {
            logger.error(e) { "Failed to register map script. Name: $scriptName" }
        }
    }
}