package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import scripting.npc.NPCScriptManager
import scripting.quest.QuestScriptManager
import tools.data.input.SeekableLittleEndianAccessor

class NPCMoreTalkHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val lastMsg = slea.readByte() // 00 last message type
        val action = slea.readByte() // 00 end chat 01 follow
        val cm = NPCScriptManager.getCM(c)
        if (cm == null || c.player?.conversation == 0) return
        if (lastMsg.toInt() == 2) {
            if (action.toInt() != 0) {
                val returnText = slea.readGameASCIIString()
                val qm = c.getQM()
                if (qm != null) {
                    qm.getText = returnText
                    if (qm.isStart) {
                        QuestScriptManager.start(c, action, lastMsg, -1)
                    } else {
                        QuestScriptManager.end(c, action, lastMsg, -1)
                    }
                } else {
                    c.getCM()?.getText = returnText
                    NPCScriptManager.action(c, action, lastMsg, -1)
                }
            } else if (c.getQM() != null) {
                c.getQM()?.dispose()
            } else {
                c.getCM()?.dispose()
            }
        } else {
            val selection = (if (slea.available() >= 4) slea.readInt()
                        else if (slea.available() > 0) slea.readByte()
                        else -1).toInt()
            val qm = c.getQM()
            if (qm != null) {
                if (qm.isStart) {
                    QuestScriptManager.start(c, action, lastMsg, selection)
                } else {
                    QuestScriptManager.end(c, action, lastMsg, selection)
                }
            } else if (c.getCM() != null) {
                NPCScriptManager.action(c, action, lastMsg, selection)
            }
        }
    }
}