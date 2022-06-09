package scripting.npc

import client.Character
import client.Client
import mu.KLoggable
import scripting.AbstractScriptManager
import javax.script.Invocable
import javax.script.ScriptContext
import javax.script.ScriptEngineManager

object NPCScriptManager : AbstractScriptManager(), KLoggable {
    override val logger = logger()
    private val cms = mutableMapOf<Client, NPCConversationManager>()
    private val scripts = mutableMapOf<Client, Invocable>()

    fun start(c: Client, npc: Int, filename: String?, chr: Character?) {
        try {
            val cm = NPCConversationManager(c, npc)
            //System.setProperty(, "true")
            if (cms.containsKey(c)) {
                dispose(c)
                return
            }
            cms[c] = cm
            val engine = ScriptEngineManager().getEngineByName("graal.js")
            val file = getScriptFileReader("npc/world${c.world}/$filename.js")
                ?: getScriptFileReader("npc/world${c.world}/$npc.js")
            if (file == null) {
                dispose(c)
                return
            }
            val binding = engine.getBindings(ScriptContext.ENGINE_SCOPE)
            binding["polyglot.js.nashorn-compat"] = true
            binding["cm"] = cm
            c.player?.conversation = 1
            engine?.eval(nashornCompat)
            engine?.eval(file)
            val iv = engine as Invocable
            scripts[c] = iv
            try {
                iv.invokeFunction("start")
            } catch (e: Exception) {
                try {
                    iv.invokeFunction("start", chr)
                } catch (e: Exception) {
                    iv.invokeFunction("action", 1.toByte(), 0.toByte(), 0)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error caused NPC: $npc" }
            notice(c, npc)
            dispose(c)
        }
    }

    fun action(c: Client, mode: Byte, type: Byte, selection: Int) {
        val iv = scripts[c]
        if (iv != null) {
            val cm = getCM(c)
            try {
                if (cm?.pendingDisposal == true) {
                    dispose(c)
                } else {
                    iv.invokeFunction("action", mode, type, selection)
                }
            } catch (e: Exception) {
                if (cm != null) {
                    logger.error(e) { "Error caused in NPC action. ${cm.npc}" }
                    notice(c, cm.npc)
                }
                dispose(c)
            }
        }
    }

    fun dispose(cm: NPCConversationManager) {
        val c = cm.c
        if (c.player?.conversation == 1) {
            c.player?.conversation = 0
        }
        cms.remove(c)
        scripts.remove(c)
        resetContext("npc/world${c.world}/${cm.npc}.js", c)
    }

    fun dispose(c: Client) {
        cms[c]?.let { dispose(it) }
    }

    fun getCM(c: Client) = cms[c]

    fun notice(c: Client, id: Int) = c.player?.dropMessage(1, "NPC에 문제가 발생하였습니다. 관리자에게 문의해주세요. $id")
}