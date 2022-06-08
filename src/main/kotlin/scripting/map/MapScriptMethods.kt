package scripting.map

import client.Client
import scripting.AbstractPlayerInteraction
import tools.packet.GameplayPacket

class MapScriptMethods(c: Client) : AbstractPlayerInteraction(c) {
    fun startExplorerExperience() {
        when (c.player?.mapId) {
            1020100 -> "swordman"
            1020200 -> "magician"
            1020300 -> "archer"
            1020400 -> "rogue"
            1020500 -> "pirate"
            else -> null
        }?.let {
            c.announce(GameplayPacket.showInfoText("Effect/Direction3.img/$it/Scene${c.player?.gender}"))
        }
    }

    fun goAdventure() {
        lockUI()
        c.announce(GameplayPacket.showIntro("Effect/Direction3.img/goAdventure/Scene${c.player?.gender}"))
    }

    fun goLith() {
        lockUI()
        c.announce(GameplayPacket.showIntro("Effect/Direction3.img/goLith/Scene${c.player?.gender}"))
    }

    /* 1.2.71
    fun explorerQuest(questId: Short, questName: String) {
        val quest = Quest.getInstance(questId.toInt())
        val player = getPlayer() ?: return
        if (!isQuestStarted(questId.toInt())) {
            if (!quest.forceStart(player, 9000066)) return
        }
        val q = player.getQuest(quest)
        if (!q.addMedalMap(player.mapId)) return
        val status = q.getMedalProgressSize().toString()
        val infoEx = quest.infoEx
        player.announce(PacketCreator.questProgress(quest.infoNumber, status))
        val smp = StringBuilder()
        val etm = StringBuilder()
        if (q.getMedalProgressSize() == infoEx) {
            etm.append("Earned the ").append(questName).append(" title!")
            smp.append("You have earned the <").append(questName).append(">").append(rewardString)
            player.announce(PacketCreator.getShowQuestCompletion(quest.id.toInt()))
        } else {
            player.announce(PacketCreator.earnTitleMessage("$status/$infoEx regions explored."))
            etm.append("Trying for the ").append(questName).append(" title.")
            smp.append("You made progress on the ").append(questName).append(" title. ").append(status).append("/")
                .append(infoEx)
        }
        player.announce(PacketCreator.earnTitleMessage(etm.toString()))
        showInfoText(smp.toString())
    }

    fun touchTheSky() { // 29004
        val quest = Quest.getInstance(29004)
        val player = getPlayer() ?: return
        if (!isQuestStarted(29004)) {
            if (!quest.forceStart(player, 9000066)) return
        }
        val q = player.getQuest(quest)
        if (!q.addMedalMap(player.mapId)) return
        val status = q.getMedalProgressSize().toString()
        player.announce(PacketCreator.questProgress(quest.infoNumber, status))
        player.announce(PacketCreator.earnTitleMessage("$status/5 Completed"))
        player.announce(PacketCreator.earnTitleMessage("The One Who's Touched the Sky title in progress."))
        if (q.getMedalProgressSize() == quest.infoEx) {
            showInfoText("The One Who's Touched the Sky$rewardString")
            player.announce(PacketCreator.getShowQuestCompletion(quest.id.toInt()))
        } else {
            showInfoText("The One Who's Touched the Sky title in progress. $status/5 Completed")
        }
    }*/
}