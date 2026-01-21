package scripting.npc

import client.*
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.ItemFactory
import client.inventory.SkinColor
import constants.ExpTable
import constants.ItemConstants
import database.Characters
import mu.KLogging
import net.server.Server
import net.server.guild.Guild
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import provider.DataProviderFactory
import scripting.AbstractPlayerInteraction
import server.InventoryManipulator
import server.InventoryManipulator.Companion.addFromDrop
import server.ItemInformationProvider
import server.ItemInformationProvider.getEquipById
import server.quest.Quest
import tools.ServerJSON.settings
import tools.packet.CashPacket
import tools.packet.GameplayPacket
import tools.packet.InteractPacket
import tools.packet.NpcPacket
import java.io.File
import java.sql.SQLException
import kotlin.random.Random

open class NPCConversationManager(c: Client, val npc: Int) : AbstractPlayerInteraction(c) {
    var getText = ""
    var pendingDisposal = false

    private fun addRandomItem(id: Int) {
        addFromDrop(c, ItemInformationProvider.randomizeStats(getEquipById(id) as Equip), true)
    }

    fun addStorage(slot: Byte, type: Byte) {
        val storage = c.player?.storage ?: return
        val item = c.player?.getInventory(InventoryType.getByType(type))?.getItem(slot)?.copy() ?: return
        val itemId = item.itemId
        var quantity = item.quantity
        if (item.itemId == itemId && (item.quantity >= quantity || ItemConstants.isRechargeable(itemId))) {
            if (ItemConstants.isRechargeable(itemId)) quantity = item.quantity
            InventoryManipulator.removeFromSlot(c, InventoryType.getByType(type), slot, quantity, false)
            item.quantity = quantity
            storage.store(item)
            storage.sendStored(c, InventoryType.getByType(type))
            c.player?.dropMessage(0, ItemInformationProvider.getName(itemId) + "아이템을 넣었습니다.")
        }
    }

    fun changeJobById(a: Int) {
        GameJob.getById(a)?.let { getPlayer()?.changeJob(it) }
        getPlayer()?.setRates()
    }

    fun completeQuest(id: Int) = getPlayer()?.let { Quest.getInstance(id).forceComplete(it, npc) }

    fun displayGuildRanks() = Guild.displayGuildRanks(c, npc)

    open fun dispose() = NPCScriptManager.dispose(this)

    fun divideTeams() = getEvent()?.let { getPlayer()?.eventTeam = it.limit % 2 }

    fun dropItem(type: Byte, i: Byte) {
        val item = c.player?.getInventory(InventoryType.getByType(type))?.getItem(i)?.copy() ?: return
        InventoryManipulator.removeFromSlot(c, InventoryType.getByType(type), i, item.quantity, false)
        c.player?.dropMessage(5, "아이템 삭제")
    }

    fun dropMessage(message: String) {
        c.player?.dropMessage(1, message)
    }

    fun gainCloseness(closeness: Int) {
        val pet = getPlayer()?.pet ?: return
        if (pet.closeness > 30000) {
            pet.closeness = 30000
            return
        }
        pet.gainCloseness(closeness)
        while (pet.closeness > ExpTable.getClosenessNeededForLevel(pet.level.toInt())) {
            pet.level = (pet.level + 1).toByte()
            c.announce(CashPacket.showOwnPetLevelUp())
            getPlayer()?.let { getPlayer()?.map?.broadcastMessage(it, CashPacket.showPetLevelUp(getPlayer())) }
        }
        val pet1 = getPlayer()?.getInventory(InventoryType.CASH)?.getItem(pet.position) ?: return
        getPlayer()?.forceUpdateItem(pet1)
    }

    fun gainExp(gain: Int) = getPlayer()?.gainExp(gain, show = true, inChat = true, white = true)

    fun gainNX(expRate: Int) = getPlayer()?.gainMPoint(expRate)

    fun gainStorageItem(items: Byte, slot: Byte) {
        val storage = c.player?.storage
        val item = storage?.getItem(slot)
        if (item != null) {
            c.player?.let { player ->
                if (ItemInformationProvider.isPickupRestricted(item.itemId) && player.getItemQuantity(item.itemId, true) > 0) {
                    player.dropMessage(6, "Error")
                }
                if (InventoryManipulator.checkSpace(c, item.itemId, item.quantity.toInt(), item.owner)) {
                    val item2 = storage.takeOut(slot)
                    if ((item2.flag and ItemConstants.KARMA) == ItemConstants.KARMA) {
                        item2.flag = (item.flag xor ItemConstants.KARMA)
                    } else if (item2.getType() == 2 && item2.flag and ItemConstants.SPIKES == ItemConstants.SPIKES) {
                        item.flag = item.flag xor ItemConstants.SPIKES
                    }
                    addFromDrop(c, item2, false)
                    storage.sendTakenOut(c, ItemInformationProvider.getInventoryType(item2.itemId))
                    player.dropMessage(0, ItemInformationProvider.getName(item.itemId) + "아이템을 찾았습니다.")
                } else {
                    player.dropMessage(6, "Error")
                }
            }
        }
    }

    fun gainMeso(gain: Int) = getPlayer()?.gainMeso(gain, show = true, enableActions = false, inChat = true)

    fun getCustomStatus(id: Int) = getPlayer()?.getCustomQuestStatus(id)

    fun getEquipChar(str: String, type: Byte): String {
        val chr = c.getChannelServer().players.getCharacterByName(str)
        val equip = chr?.getInventory(InventoryType.EQUIPPED)
        val type1 = equip?.getItem(type)
        return if (type1 != null) {
            "#i${type1.itemId}#"
        } else ""
    }

    private fun getEvent() = c.getChannelServer().event

    fun getName() = getPlayer()?.name

    fun getSItem(id: Int, str: Int, dex: Int, luk: Int, `in`: Int, wd: Int, md: Int, up: Int) {
        val origin = getEquipById(id) as Equip //오리지널 옵션
        origin.str = (str + origin.str).toShort()
        origin.dex = (dex + origin.dex).toShort()
        origin.luk = (luk + origin.luk).toShort()
        origin.int = (`in` + origin.int).toShort()
        origin.watk = (wd + origin.watk).toShort()
        origin.matk = (md + origin.matk).toShort()
        origin.upgradeSlots = (up + origin.upgradeSlots).toByte()
        c.player?.let {
            if ((it.getInventory(InventoryType.EQUIP)?.getNumFreeSlot() ?: 0) < 1) {
                it.dropMessage(6, "장비창을 1칸이상 비워주세요.")
                return
            }
        }
        addFromDrop(c, origin, true)
    }

    fun getGender() = getPlayer()?.gender

    fun getHp() = getPlayer()?.hp

    fun getImageChar(mode: Byte, type: Byte, sel: Int? = null): String {
        val equip = c.player?.getInventory(InventoryType.getByType(mode))
        val item = equip?.getItem(type)
        return if (item != null && item.itemId !in 2060000..2080000) { // && equip.getItem(type).getQuantity() <= 100) {
            (if (sel != null) "#L$sel##i" else "#i") + item.itemId + if (sel != null) "##l" else "#"
        } else ""
    }

    fun getItemEffect(itemId: Int) = ItemInformationProvider.getItemEffect(itemId)

    fun getJobId() = getPlayer()?.job?.id

    fun getJobName(id: Int) = GameJob.getById(id)

    fun getLevel() = getPlayer()?.level

    override fun getParty() = getPlayer()?.party

    private fun getPartyMembers(): List<Character> {
        val chars = mutableListOf<Character>()
        getPlayer()?.party?.let { party ->
            getPlayer()?.world?.let {
                Server.getChannelsFromWorld(it).forEach { ch ->
                    ch.getPartyMembers(party).forEach { it1 ->
                        chars.add(it1)
                    }
                }
            }
        } ?: return emptyList()
        return chars
    }

    fun getMeso() = getPlayer()?.meso?.get() ?: 0

    fun hasMerchant() = getPlayer()?.hasMerchant

    fun hasMerchantItems(): Boolean {
        return if (getPlayer()?.id?.let { ItemFactory.MERCHANT.loadItems(it, false).isNotEmpty() } == true) true
        else getPlayer()?.merchantMeso != 0
    }

    override fun haveItem(itemId: Int) = getPlayer()?.haveItem(itemId) ?: false

    fun haveItem(itemId: Int, quantity: Int) = (getPlayer()?.getItemQuantity(itemId, false) ?: 0) >= quantity

    fun itemQuantity(itemId: Int) = getPlayer()?.getInventory(ItemInformationProvider.getInventoryType(itemId))?.countById(itemId) ?: 0

    fun maxMastery() {
        DataProviderFactory.getDataProvider(File("${settings.wzPath}/String.wz")).getData("Skill.img")?.children?.forEach { skill ->
            val sId = skill.name.toIntOrNull() ?: return@forEach
            val s = SkillFactory.getSkill(sId) ?: return@forEach
            getPlayer()?.changeSkillLevel(s, 0, s.getMaxLevel(), -1)
        }
    }

    fun partyMembersInMap(): Int {
        var inMap = 0
        getPlayer()?.map?.characters?.forEach {
            if (it.party == getPlayer()?.party) inMap++
        }
        return inMap
    }

    fun processGachapon(id: Array<Int>, remote: Boolean) {
        val map = listOf(100000000, 101000000, 102000000, 103000000, 105040300, 800000000, 809000101, 809000201, 600000000, 120000000)
        val itemId = id[Random.nextInt(id.size - 1)]
        addRandomItem(itemId)
        if (!remote) gainItem(5220000, -1)
        sendNext("#b#t$itemId##k을(를) 획득했습니다.")
        c.getChannelServer().broadcastPacket(
            InteractPacket.gachaponMessage(
                getPlayer()?.getInventory(InventoryType.getByType((itemId / 1000000).toByte()))?.findById(itemId),
                c.getChannelServer().mapFactory.getMap(map[if (npc != 9100117 && npc != 9100109) npc - 9100100 else if (npc == 9100109) 8 else 9]).mapName,
                getPlayer()
            )
        )
    }

    override fun resetMap(mapId: Int) = c.getChannelServer().mapFactory.getMap(mapId).resetReactors()

    fun resetStats() = getPlayer()?.resetStats()

    fun sendAcceptDecline(text: String) = c.announce(NpcPacket.getNpcTalk(npc, 0x0c, text, ""))

    fun sendGetNumber(text: String, def: Int, min: Int, max: Int) = c.announce(NpcPacket.getNpcTalkNum(npc, text, def, min, max))

    fun sendGetText(text: String) = c.announce(NpcPacket.getNpcTalkText(npc, text, ""))

    fun sendNext(text: String) = c.announce(NpcPacket.getNpcTalk(npc, 0, text, "00 01"))

    fun sendNextPrev(text: String) = c.announce(NpcPacket.getNpcTalk(npc, 0, text, "01 01"))

    fun sendOk(text: String) = c.announce(NpcPacket.getNpcTalk(npc, 0, text, "00 00"))

    fun sendPrev(text: String) = c.announce(NpcPacket.getNpcTalk(npc, 0, text, "01 00"))

    fun sendSimple(text: String) = c.announce(NpcPacket.getNpcTalk(npc, 4, text, ""))

    fun sendStorage() {
        c.player?.conversation = 4
        c.player?.storage?.sendStorage(c, npc)
    }

    fun sendStyle(text: String, styles: Array<Int>) = c.announce(NpcPacket.getNpcTalkStyle(npc, text, styles))

    fun sendYesNo(text: String) = c.announce(NpcPacket.getNpcTalk(npc, 1, text, ""))

    fun setCustomStatus(id: Int, stat: Int) = getPlayer()?.setCustomQuestStatus(id, stat)

    fun setFace(face: Int) {
        getPlayer()?.face = face
        getPlayer()?.updateSingleStat(CharacterStat.FACE, face, false)
        getPlayer()?.equipChanged()
    }

    fun setHair(hair: Int) {
        getPlayer()?.hair = hair
        getPlayer()?.updateSingleStat(CharacterStat.HAIR, hair)
        getPlayer()?.equipChanged()
    }

    fun setSkin(color: Int) {
        getPlayer()?.skinColor = SkinColor.getById(color) ?: SkinColor.NORMAL
        getPlayer()?.updateSingleStat(CharacterStat.SKIN, color)
        getPlayer()?.equipChanged()
    }

    fun showEffect(effect: String) = getPlayer()?.map?.broadcastGMMessage(GameplayPacket.environmentChange(effect, 3))

    fun showFredrick() = c.announce(CashPacket.getFredrick(getPlayer()))

    fun startQuest(id: Int) = getPlayer()?.let { Quest.getInstance(id).forceStart(it, npc) }

    fun warpParty(id: Int) {
        getPartyMembers().forEach {
            // if (id == 925020100) it.dojoParty = true
            it.changeMap(getWarpMap(id), null)
        }
    }

    fun safeDispose() {
        pendingDisposal = true
    }

    companion object : KLogging() {
        fun ranking(rk: Boolean): ResultRow? {
            var rs: ResultRow? = null
            try {
                transaction {
                    rs = if (!rk) {
                        Characters.selectAll().where {
                            (Characters.gm less 1)
                        }.orderBy(Characters.level, SortOrder.DESC).limit(10).first()
                    } else {
                        Characters.selectAll().where { Characters.gm greaterEq 1 }.first()
                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to get characters ranking in level." }
            }
            return rs
        }

        fun dropItemList(c: Client, ty: Byte): String {
            val equip = c.player?.getInventory(InventoryType.getByType(ty)) ?: return ""
            return buildString {
                equip.list().forEach {
                    append("#L${it.position}##v${it.itemId}##l")
                }
            }
        }

        fun itemList(c: Client, ty: Byte): String {
            val equip = c.player?.getInventory(InventoryType.getByType(ty)) ?: return ""
            return buildString {
                equip.list().forEach {
                    append("#L${it.position}##v${it.itemId}##l")
                }
            }
        }

        fun storageItemList(c: Client, type: Byte): String {
            c.player?.let { player ->
                return buildString {
                    player.storage?.items?.forEachIndexed { i, item ->
                        append("#L$i##v${item.itemId}##l")
                    }
                }
            } ?: return ""
        }
    }
}