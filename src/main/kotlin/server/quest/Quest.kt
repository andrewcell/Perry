package server.quest

import client.Character
import client.QuestStatus
import provider.DataProvider
import provider.DataProviderFactory
import provider.DataTool
import tools.PacketCreator
import tools.ServerJSON.settings
import tools.packet.CharacterPacket
import tools.packet.GameplayPacket
import java.io.File

class Quest(val id: Short) {
    var infoNumber: Short = 0
    var infoEx = 0
    var timeLimit: Int = 0
    private var timeLimit2: Int = 0
    private val startReqs = mutableListOf<QuestRequirement>()
    private val completeReqs = mutableListOf<QuestRequirement>()
    private val startActs = mutableListOf<QuestAction>()
    private val completeActs = mutableListOf<QuestAction>()
    val relevantMobs = mutableListOf<Int>()
    private var autoStart = false
    private var autoPreComplete = false
    private var repeatable = false

    init {
        val reqData = requirements?.getChildByPath(id.toString())
        val startReqData = reqData?.getChildByPath("0")
        startReqData?.children?.forEach { startReq ->
            val type = QuestRequirementType.getByWzName(startReq.name)
            if (type == QuestRequirementType.INTERVAL) repeatable  = true
            val req = QuestRequirement(this, type, startReq)
            if (req.type == QuestRequirementType.MOB) {
                startReq.children.forEach { mob ->
                    relevantMobs.add(DataTool.getInt(mob.getChildByPath("id")))
                }
            }
            startReqs.add(req)
        }
        val completeReqData = reqData?.getChildByPath("1")
        completeReqData?.children?.forEach { completeReq ->
            val req = QuestRequirement(this, QuestRequirementType.getByWzName(completeReq.name), completeReq)
            if (req.type == QuestRequirementType.INFO_NUMBER) infoNumber = DataTool.getInt(completeReq, 0).toShort()
            if (req.type == QuestRequirementType.INFO_EX) {
                val zero = completeReq.getChildByPath("0")
                if (zero != null) {
                    val value = zero.getChildByPath("value")
                    if (value != null) {
                        infoEx = DataTool.getString(value, "0").toInt()
                    }
                }
            }
            if (req.type == QuestRequirementType.MOB) {
                completeReq.children.forEach { mob ->
                    relevantMobs.add(DataTool.getInt(mob.getChildByPath("id")))
                }
                relevantMobs.sort()
            }
            completeReqs.add(req)
        }
        val actData = actions?.getChildByPath(id.toString())
        val startActData = actData?.getChildByPath("0")
        startActData?.children?.forEach { startAct ->
            val questActionType = QuestActionType.getByWzName(startAct.name)
            startActs.add(QuestAction(questActionType, startAct, this))
        }
        val completeActData = actions?.getChildByPath(id.toString())?.getChildByPath("1")
        completeActData?.children?.forEach { completeAct ->
            completeActs.add(QuestAction(QuestActionType.getByWzName(completeAct.name), completeAct, this))
        }
        val questInfo = info?.getChildByPath(id.toString())
        timeLimit = DataTool.getInt("timeLimit", questInfo, 0)
        timeLimit2 = DataTool.getInt("timeLimit2", questInfo, 0)
        autoStart = DataTool.getInt("autoStart", questInfo, 0) == 1
        autoPreComplete = DataTool.getInt("autoPreComplete", questInfo, 0) == 1
    }

    private fun canStart(c: Character, npcId: Int): Boolean {
        if (c.getQuest(this).status != QuestStatus.Status.NOT_STARTED && !(c.getQuest(this).status == QuestStatus.Status.COMPLETED && repeatable)) {
            return false
        }
        startReqs.forEach { if (!it.check(c, npcId)) return false }
        return true
    }

    fun canComplete(c: Character, npcId: Int?): Boolean {
        if (c.getQuest(this).status != QuestStatus.Status.STARTED) return false
        completeReqs.forEach {
            if (!it.check(c, npcId)) return false
        }
        return true
    }

    fun start(c: Character, npc: Int) {
        if ((autoStart || checkNpcOnMap(c, npc)) && canStart(c, npc)) {
            startActs.forEach {
                it.run(c, null)
            }
            forceStart(c, npc)
        }
    }

    fun complete(c: Character, npc: Int, selection: Int? = null) {
        if ((autoPreComplete || checkNpcOnMap(c, npc)) && canComplete(c, npc)) {
            for (it in completeActs) { if (!it.check(c, selection)) return }
            completeActs.forEach { it.run(c, selection) }
            forceComplete(c, npc)
            c.announce(PacketCreator.showSpecialEffect(9))
            c.map.broadcastMessage(CharacterPacket.showForeignEffect(c.id, 9))
        }
    }

    fun reset(c: Character) = c.updateQuest(QuestStatus(this, QuestStatus.Status.NOT_STARTED))

    fun forfeit(c: Character) {
        if (c.getQuest(this).status != QuestStatus.Status.STARTED) return
        if (timeLimit > 0) c.announce(GameplayPacket.removeQuestTimeLimit(id))
        val newStatus = QuestStatus(this, QuestStatus.Status.NOT_STARTED)
        newStatus.forfeited = c.getQuest(this).forfeited + 1
        c.updateQuest(newStatus)
    }

    fun forceStart(c: Character, npc: Int): Boolean {
        if (!canStart(c, npc)) return false
        val newStatus = QuestStatus(this, QuestStatus.Status.STARTED, npc)
        newStatus.forfeited = c.getQuest(this).forfeited
        if (timeLimit >  0) c.questTimeLimit(this, 30000)
        c.updateQuest(newStatus)
        return true
    }

    fun forceComplete(c: Character, npc: Int): Boolean {
        val newStatus = QuestStatus(this, QuestStatus.Status.COMPLETED, npc)
        newStatus.forfeited = c.getQuest(this).forfeited
        newStatus.completionTime = System.currentTimeMillis()
        c.updateQuest(newStatus)
        return true
    }

    fun getItemAmountNeeded(itemId: Int): Int {
        val data = requirements?.getChildByPath(id.toString())?.getChildByPath("1") ?: return 0
        for (req in data.children) {
            val type = QuestRequirementType.getByWzName(req.name)
            if (type != QuestRequirementType.ITEM) continue
            req.children.forEach { d ->
                if (DataTool.getInt(d.getChildByPath("id"), 0) == itemId) {
                    return DataTool.getInt(d.getChildByPath("count"), 0)
                }
            }
        }
        return 0
    }

    fun getMobAmountNeeded(mid: Int): Int {
        val data = requirements?.getChildByPath(id.toString())?.getChildByPath("1") ?: return 0
        for (req in data.children) {
            val type = QuestRequirementType.getByWzName(req.name)
            if (type != QuestRequirementType.MOB) continue
            req.children.forEach { d ->
                if (DataTool.getInt(d.getChildByPath("id"), 0) == mid) {
                    return DataTool.getInt(d.getChildByPath("count"), 0)
                }
            }
        }
        return 0
    }

    private fun checkNpcOnMap(player: Character, npcId: Int) = player.map.containsNpc(npcId)

    companion object {
        private val quests = mutableMapOf<Int, Quest>()
        private val questData: DataProvider = DataProviderFactory.getDataProvider(File("${settings.wzPath}/Quest.wz"))
        private val requirements = questData.getData("Check.img")
        private val actions = questData.getData("Act.img")
        private val info = questData.getData("QuestInfo.img")

        fun getInstance(id: Int): Quest {
            var ret = quests[id]
            if (ret == null) {
               ret = Quest(id.toShort())
               quests[id] = ret
            }
            return ret
        }
    }
}