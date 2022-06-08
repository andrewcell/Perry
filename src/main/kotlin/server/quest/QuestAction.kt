package server.quest

import client.*
import client.inventory.InventoryType.*
import constants.ItemConstants
import constants.ServerConstants
import provider.Data
import provider.DataTool
import server.InventoryManipulator
import server.ItemInformationProvider
import server.quest.QuestActionType.*
import tools.packet.GameplayPacket
import tools.packet.InteractPacket
import tools.packet.ItemPacket
import kotlin.random.Random

class QuestAction(val type: QuestActionType, val data: Data, val quest: Quest) {
    fun check(c: Character, extSelection: Int?): Boolean {
        when (type) {
            ITEM -> {
                val props = mutableMapOf<Int, Int>()
                data.children.forEach { entry ->
                    val prop = entry.getChildByPath("prop")
                    if (prop != null && DataTool.getInt(prop) != -1 && canGetItem(entry, c)) {
                        for (i in 0 until DataTool.getInt(entry.getChildByPath("prop"))) {
                            props[props.size] = DataTool.getInt(entry.getChildByPath("id"))
                        }
                    }
                }
                var extNum = 0
                val selection = if (props.isNotEmpty()) props[Random.nextInt(props.size)] else 0
                var eq = 0
                var use = 0
                var setup = 0
                var etc = 0
                var cash = 0
                data.children.forEach {
                    if (!canGetItem(it, c)) return@forEach
                    val id = DataTool.getInt(it.getChildByPath("id"), -1)
                    val prop = it.getChildByPath("prop")
                    if (prop != null) {
                        if (DataTool.getInt(prop) == -1) {
                            if (extSelection != extNum++) return@forEach
                        } else if (id != selection) return@forEach
                    }
                    when (ItemInformationProvider.getInventoryType(id)) {
                        EQUIP -> eq++
                        USE -> use++
                        SETUP -> setup++
                        ETC -> etc++
                        CASH -> cash++
                        else -> {}
                    }
                }
                if (c.getInventory(EQUIP)?.getNumFreeSlot()!! <= eq) {
                    c.dropMessage(1, "퀘스트 수행을 하실려면 장비칸을 비워주세요.")
                    return false
                }
                if (c.getInventory(USE)!!.getNumFreeSlot() <= use) {
                    c.dropMessage(1, "퀘스트 수행을 하실려면 소비칸을 비워주세요.")
                    return false
                }
                if (c.getInventory(SETUP)!!.getNumFreeSlot() <= setup) {
                    c.dropMessage(1, "퀘스트 수행을 하실려면 설치칸을 비워주세요.")
                    return false
                }
                if (c.getInventory(ETC)!!.getNumFreeSlot() <= etc) {
                    c.dropMessage(1, "퀘스트 수행을 하실려면 기타칸을 비워주세요.")
                    return false
                }
                if (c.getInventory(CASH)!!.getNumFreeSlot() <= cash) {
                    c.dropMessage(1, "퀘스트 수행을 하실려면 캐시칸을 비워주세요.")
                    return false
                }
            }
            MESO -> {
                val meso = DataTool.getInt(data, 0)
                if (c.meso.get() + meso < 0) {
                    c.dropMessage(1, "메소 최대 소지한도를 초과했습니다.")
                    return false
                }
                return true
            }
            else -> return true
        }
        return true
    }

    private fun canGetItem(item: Data, c: Character): Boolean {
        val genderChild = item.getChildByPath("gender") ?: return true
        val jobChild = item.getChildByPath("job") ?: return true
        val gender = DataTool.getInt(genderChild)
        val job = DataTool.getInt(jobChild)
        if (gender != 2 && gender != c.gender) return false
        if (job < 100) {
            if (GameJob.getBy5ByteEncoding(job).id / 100 != c.job.id / 100) return false
        } else if (job != c.job.id) return false
        return true
    }

    fun run(c: Character, extSelection: Int?) {
        val status = c.getQuest(quest)
        run whn@ {
            when (type) {
                EXP -> {
                    if (status.status == QuestStatus.Status.NOT_STARTED && status.forfeited > 0) {
                        return@whn
                    }
                    if (c.isBeginnerJob()) {
                        c.gainExp(DataTool.getInt(data), show = true, inChat = true)
                    } else {
                        c.gainExp(DataTool.getInt(data) * ServerConstants.questExpRate, show = true, inChat = true)
                    }
                }
                ITEM -> {
                    val props = mutableMapOf<Int, Int>()
                    run each1@ {
                        data.children.forEach { entry ->
                            val prop = entry.getChildByPath("prop") ?: return@forEach
                            if (DataTool.getInt(prop) != -1 && canGetItem(entry, c)) {
                                for (i in 0 until DataTool.getInt(entry.getChildByPath("prop"))) {
                                    props[props.size] = DataTool.getInt(entry.getChildByPath("id"))
                                }
                            }
                        }
                    }
                    val selection = if (props.isNotEmpty()) props[Random.nextInt(props.size)] else 0
                    var extNum = 0
                    run each2@{
                        data.children.forEach { entry ->
                            if (!canGetItem(entry, c)) return@forEach
                            val prop = entry.getChildByPath("prop")
                            if (prop != null) {
                                if (DataTool.getInt(prop) == -1) {
                                    if (extSelection != extNum++) return@forEach
                                } else if (DataTool.getInt(entry.getChildByPath("id")) != selection) return@forEach
                            }
                            val count = DataTool.getInt(entry.getChildByPath("count"), 0)
                            val itemId = DataTool.getInt(entry.getChildByPath("id"))
                            if (count < 0) {
                                val type = ItemInformationProvider.getInventoryType(itemId)
                                val quantity = count * -1
                                InventoryManipulator.removeById(c.client, type, itemId, quantity,
                                    fromDrop = true,
                                    consume = false
                                )
                                c.client.announce(ItemPacket.getShowItemGain(itemId, count.toShort(), true))
                            } else {
                                if ((c.getInventory(ItemInformationProvider.getInventoryType(itemId))?.getNextFreeSlot()
                                        ?: -1) > -1
                                ) {
                                    InventoryManipulator.addById(c.client, itemId, count.toShort())
                                    c.client.announce(ItemPacket.getShowItemGain(itemId, count.toShort(), true))
                                }
                            }
                        }
                    }
                }
                NEXT_QUEST -> {
                    val nextQuest = DataTool.getInt(data)
                    if (status.status == QuestStatus.Status.NOT_STARTED && status.forfeited > 0) return@whn
                    c.client.announce(GameplayPacket.updateQuestFinish(quest.id, status.npc, nextQuest.toShort()))
                }
                MESO -> {
                    if (status.status == QuestStatus.Status.NOT_STARTED && status.forfeited > 0) return@whn
                    c.gainMeso(DataTool.getInt(data) * ServerConstants.questMesoRate,
                        show = true,
                        enableActions = false,
                        inChat = true
                    )
                }
                QUEST -> {
                    data.forEach { entry ->
                        val questId = DataTool.getInt(entry.getChildByPath("id"))
                        val state = DataTool.getInt(entry.getChildByPath("state"))
                        c.updateQuest(QuestStatus(Quest.getInstance(questId), (QuestStatus.Status.getById(state) ?: QuestStatus.Status.NOT_STARTED)))
                    }
                }
                SKILL -> {
                    data.forEach { entry ->
                        val skillId = DataTool.getInt(entry.getChildByPath("id"))
                        var skillLevel = DataTool.getInt(entry.getChildByPath("skillLevel"))
                        var masterLevel = DataTool.getInt(entry.getChildByPath("masterLevel"))
                        val skillObject = SkillFactory.getSkill(skillId) ?: return@forEach
                        var shouldLearn = false
                        val applicableJobs = entry.getChildByPath("job")
                        run jobEach@{
                            applicableJobs?.forEach {
                                val job = GameJob.getById(DataTool.getInt(it))
                                if (c.job == job) {
                                    shouldLearn = true
                                    return@jobEach
                                }
                            }
                        }
                        if (skillObject.isBeginnerSkill()) shouldLearn = true
                        skillLevel = skillLevel.coerceAtLeast(c.getSkillLevel(skillObject).toInt())
                        masterLevel = masterLevel.coerceAtLeast(c.getMasterLevel(skillObject))
                        if (shouldLearn) c.changeSkillLevel(skillObject, skillLevel.toByte(), masterLevel, -1)
                    }
                }
                FAME -> {
                    if (status.status == QuestStatus.Status.NOT_STARTED && status.forfeited > 0) return@whn
                    val fameToGain = DataTool.getInt(data)
                    c.addFame(fameToGain)
                    c.updateSingleStat(CharacterStat.FAME, c.fame)
                    c.client.announce(InteractPacket.getShowFameGain(fameToGain))
                }
                BUFF -> {
                    if (status.status == QuestStatus.Status.NOT_STARTED && status.forfeited > 0) return@whn
                    ItemInformationProvider.getItemEffect(DataTool.getInt(data))?.applyTo(c)
                }
                PETSKILL -> {
                    if (status.status == QuestStatus.Status.NOT_STARTED && status.forfeited > 0) return@whn
                    val flag = DataTool.getInt("petskill", data)
                    c.pet?.flag = ItemConstants.getFlagByInt(flag)
                }
                else -> return
            }
        }
    }
}