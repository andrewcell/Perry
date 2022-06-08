package scripting.portal

import client.Client
import mu.KLoggable
import server.Portal
import java.io.File
import java.io.FileReader
import javax.script.Compilable
import javax.script.Invocable
import javax.script.ScriptEngineManager

object PortalScriptManager : KLoggable {
    override val logger = logger()
    private val scripts = mutableMapOf<String, PortalScript?>()
    private val sef = ScriptEngineManager().getEngineByName("graal.js").factory

    private fun getPortalScript(scriptName: String): PortalScript? {
        val exists = scripts[scriptName]
        if (exists != null) return exists
        val scriptFile = File("scripts/portal/$scriptName.js")
        if (!scriptFile.exists()) {
            scripts[scriptName] = null
            return null
        }
        val portal = sef.scriptEngine
        try {
            val fr = FileReader(scriptFile)
            portal as Compilable
            portal.compile(fr).eval()
        } catch (e: Exception) {
            logger.error(e) { "Portal script error. $scriptName "}
        }
        val script = (portal as Invocable).getInterface(PortalScript::class.java)
        scripts[scriptName] = script
        return script
    }

    fun executePortalScript(portal: Portal, c: Client): Boolean {
        try {
            val script = portal.scriptName?.let { getPortalScript(it) }
            if (script != null) {
                return script.enter(PortalPlayerInteraction(c, portal))
            }
        } catch (e: Exception) {
            logger.error(e) { "${portal.scriptName} caused execute script error." }
        }
        return false
    }

    fun reloadPortalScripts() = scripts.clear()
}