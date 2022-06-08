package scripting.map

import client.Client
import mu.KLoggable
import java.io.File
import java.io.FileReader
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptEngineManager

object MapScriptManager : KLoggable {
    override val logger = logger()
    private val scripts = mutableMapOf<String, Invocable>()
    private val sef = ScriptEngineManager().getEngineByName("graal.js").factory

    fun reloadScripts() = scripts.clear()

    fun scriptExists(scriptName: String, firstUser: Boolean): Boolean {
        val user = if (firstUser) "onFirstUserEnter" else "onUserEnter"
        val scriptFile = File("scripts/map/$user/$scriptName.js")
        return scriptFile.exists()
    }

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