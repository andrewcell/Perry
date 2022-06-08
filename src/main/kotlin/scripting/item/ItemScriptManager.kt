package scripting.item

import client.Client
import mu.KLoggable
import tools.PacketCreator
import java.io.File
import java.io.FileReader
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptEngineManager

object ItemScriptManager : KLoggable {
    override val logger = logger()
    private val scripts = mutableMapOf<String, Invocable>()
    private val sef = ScriptEngineManager().getEngineByName("graal.js").factory

    fun scriptExists(scriptName: String): Boolean {
        val scriptFile = File("scripts/item/$scriptName.js")
        return scriptFile.exists()
    }

    fun getItemScript(c: Client, scriptName: String) {
        try {
            if (scripts.containsKey(scriptName)) {
                scripts[scriptName]?.invokeFunction("start", ItemScriptMethods(c))
                return
            }
            val scriptFile = File("scripts/item/$scriptName.js")
            if (!scriptFile.exists()) {
                c.announce(PacketCreator.enableActions())
                return
            }
            val portal = sef.scriptEngine
            val fr = FileReader(scriptFile)
            portal as Compilable
            portal.compile(fr).eval()
            portal as Invocable
            scripts[scriptName] = portal
            portal.invokeFunction("start", ItemScriptMethods(c))
        } catch (e: Exception) {
            logger.error {"Error caused execute item script. Name: $scriptName" }
        }
    }
}