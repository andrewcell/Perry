package scripting.quest

import client.Client
import scripting.npc.NPCConversationManager
import server.quest.Quest

class QuestActionManager(c: Client, val quest: Int, npc: Int, val isStart: Boolean) : NPCConversationManager(c, npc) {
    fun forceStartQuest(id: Int) = getPlayer()?.let { Quest.getInstance(id).forceStart(it, npc) } ?: false

    fun forceCompleteQuest(id: Int) = getPlayer()?.let { Quest.getInstance(id).forceComplete(it, npc) } ?: false

    override fun dispose() {
        QuestScriptManager.dispose(this, c)
    }
}