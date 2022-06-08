package server.quest

import client.Character
import client.GameJob
import client.QuestStatus
import client.inventory.InventoryType
import provider.Data
import provider.DataTool
import server.ItemInformationProvider
import server.quest.QuestRequirementType.*
import java.util.*

class QuestRequirement(val quest: Quest, val type: QuestRequirementType, val data: Data) {
    fun check(c: Character, npcId: Int?): Boolean {
        when (type) {
            END_DATE -> {
                val timeStr = DataTool.getString(data) ?: return false
                val cal = Calendar.getInstance()
                cal[timeStr.substring(0, 4).toInt(), timeStr.substring(4, 6).toInt(), timeStr.substring(6, 8)
                    .toInt(), timeStr.substring(8, 10).toInt()] = 0
                return cal.timeInMillis >= System.currentTimeMillis()
            }
            FIELD_ENTER -> {
                val zeroField = data.getChildByPath("0")
                return if (zeroField != null) DataTool.getInt(zeroField) == c.mapId else false
            }
            INTERVAL -> return c.getQuest(quest).status == QuestStatus.Status.COMPLETED
            ITEM -> {
                data.children.forEach { itemEntry ->
                    val itemId = DataTool.getInt(itemEntry.getChildByPath("id"))
                    var quantity = 0
                    val itemType = ItemInformationProvider.getInventoryType(itemId)
                    c.getInventory(itemType)?.listById(itemId)?.forEach {
                        quantity += it.quantity
                    }
                    if (itemType == InventoryType.EQUIP) {
                        c.getInventory(InventoryType.EQUIPPED)?.listById(itemId)?.forEach {
                            quantity += it.quantity
                        }
                    }
                    val count = itemEntry.getChildByPath("count")
                    if (count != null) {
                        if (quantity < DataTool.getInt(count, 0)) return false
                    } else {
                        if (quantity != 0) return false
                    }
                }
                return true
            }
            JOB -> {
                data.children.forEach {
                    if (c.job == GameJob.getById(DataTool.getInt(it))) return true
                }
            }
            QUEST -> {
                data.children.forEach {
                    val status = c.getQuest(Quest.getInstance(DataTool.getInt(it.getChildByPath("id"))))
                    if (status == null && QuestStatus.Status.getById(DataTool.getInt(it.getChildByPath("state"))) == QuestStatus.Status.NOT_STARTED) {
                        return@forEach
                    }
                    if (status == null || status.status != QuestStatus.Status.getById(DataTool.getInt(it.getChildByPath("state")))) {
                        return false
                    }
                }
            }
            MAX_LEVEL -> return c.level <= DataTool.getInt(data)
            MIN_LEVEL -> return c.level >= DataTool.getInt(data)
            MIN_PET_TAMENESS -> return c.pet?.let { it.closeness >= DataTool.getInt(data) } ?: return false
            MOB -> {
                data.children.forEach {
                    val mobId = DataTool.getInt(it.getChildByPath("id"))
                    val killReq = DataTool.getInt(it.getChildByPath("count"))
                    if (c.getQuest(quest).getProgress(mobId).toInt() < killReq) return false
                }
            }
            //MONSTER_BOOK -> return c.monsterBook.totalCards >= DataTool.getInt(data)
            NPC -> return npcId == null || npcId == DataTool.getInt(data)
            INFO_EX -> return c.getQuest(quest).medalProgress.size >= quest.infoEx
            COMPLETED_QUEST -> return c.getCompletedQuests().size >= DataTool.getInt(data)
            else -> return true
        }
        return true
    }

    fun getQuestItemsToShowOnlyIfQuestIsActivated(): List<Int> {
        if (type != ITEM) return emptyList()
        val delta = mutableListOf<Int>()
        data.children.forEach {
            val itemId = DataTool.getInt(it.getChildByPath("id"))
            if (ItemInformationProvider.isQuestItem(itemId)) delta.add(itemId)
        }
        return delta
    }
}