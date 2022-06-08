package net.server.channel.handlers

import server.quest.Quest
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import client.Client
import scripting.quest.QuestScriptManager

class QuestActionHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val action = slea.readByte().toInt()
        val questId = slea.readShort()
        val player = c.player ?: return
        val quest = Quest.getInstance(questId.toInt())
        when (action) {
            1 -> { //Start Quest
                val npc = slea.readInt()
                if (slea.available() >= 4) {
                    slea.readInt()
                }
                quest.start(player, npc)
            }
            2 -> { // Complete Quest
                val npc = slea.readInt()
                slea.readInt()
                if (slea.available() >= 2) {
                    val selection = slea.readShort().toInt()
                    quest.complete(player, npc, selection)
                } else {
                    quest.complete(player, npc, null)
                }
            }
            3 -> { // forfeit quest
                quest.forfeit(player)
            }
            4 -> { // scripted start quest
                //System.out.println(slea.toString());
                val npc = slea.readInt()
                slea.readInt()
                QuestScriptManager.start(c, questId, npc)
            }
            5 -> { // scripted end quests
                //System.out.println(slea.toString());
                val npc = slea.readInt()
                slea.readInt()
                QuestScriptManager.end(c, questId, npc)
            }
        }
    }
}