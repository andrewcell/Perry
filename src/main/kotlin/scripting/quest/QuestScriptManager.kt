package scripting.quest

import client.Client
import client.QuestStatus
import mu.KLoggable
import scripting.AbstractScriptManager
import server.quest.Quest
import javax.script.Invocable
import javax.script.ScriptEngineManager

object QuestScriptManager : AbstractScriptManager(), KLoggable {
    override val logger = logger()
    private val qms = mutableMapOf<Client, QuestActionManager>()
    private val scripts = mutableMapOf<Client, Invocable>()
    private val sem = ScriptEngineManager()

    fun start(c: Client, questId: Short, npc: Int) {
        val quest = Quest.getInstance(questId.toInt())
        if (c.player?.getQuest(quest)?.status != QuestStatus.Status.NOT_STARTED || c.player?.map?.containsNpc(npc) != true) {
            dispose(c)
            return
        }
        try {
            val qm = QuestActionManager(c, questId.toInt(), npc, true)
            if (qms.containsKey(c)) return
            qms[c] = qm
            val engine = sem.getEngineByName("graal.js")
            val file = getScriptFileReader("quest/$questId.js")
            engine.eval(nashornCompat)
            if (file == null) {
                qm.dispose()
                return
            }
            c.player?.conversation = 1
            engine.put("qm", qm)
            engine.eval(file)
            engine as Invocable
            scripts[c] = engine
            engine.invokeFunction("start", 1, 0, 0)
        } catch (e: Exception) {
            logger.error(e) { "Failed to start quest script $questId" }
            dispose(c)
        }
    }

    fun start(c: Client, mode: Byte, type: Byte, selection: Int) {
        val iv = scripts[c]
        if (iv != null) {
            try {
                iv.invokeFunction("start", mode, type, selection)
            } catch (e: Exception) {
                logger.error(e) { "Failed to start quest." }
                dispose(c)
            }
        }
    }

    fun end(c: Client, questId: Short, npc: Int) {
        val quest = Quest.getInstance(questId.toInt())
        if (c.player?.getQuest(quest)?.status != QuestStatus.Status.STARTED || c.player?.map?.containsNpc(npc) != true) {
            dispose(c)
            return
        }
        try {
            val qm = QuestActionManager(c, questId.toInt(), npc, false)
            if (qms.containsKey(c)) return
            qms[c] = qm
            val engine = sem.getEngineByName("graal.js")
            engine.eval(nashornCompat)
            val file = getScriptFileReader("quest/$questId.js")
            if (file == null) {
                qm.dispose()
                return
            }
            c.player?.conversation = 1
            engine.put("qm", qm)
            engine.eval(file)
            engine as Invocable
            scripts[c] = engine
            engine.invokeFunction("end", 1, 0, 0)
        } catch (e: Exception) {
            logger.error(e) { "Failed to end quest script $questId" }
            dispose(c)
        }
    }

    fun end(c: Client, mode: Byte, type: Byte, selection: Int) {
        val iv = scripts[c] ?: return
        try {
            iv.invokeFunction("end", mode, type, selection)
        } catch (e: Exception) {
            logger.error(e) { "Failed to end quest." }
            dispose(c)
        }
    }

    fun dispose(qm: QuestActionManager, c: Client) {
        if (c.player?.conversation == 1) {
            c.player?.conversation = 0
        }
        qms.remove(c)
        scripts.remove(c)
        resetContext("quest/${qm.quest}.js", c)
    }

    fun dispose(c: Client) {
        val qm = qms[c]
        if (qm != null) {
            dispose(qm, c)
        }
    }

    fun getQM(c: Client) = qms[c]
}