package scripting

import client.Character
import client.Client
import client.QuestStatus
import client.SkillFactory
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.ModifyInventory
import client.inventory.Pet.Companion.createPet
import constants.ItemConstants
import net.server.Server
import scripting.npc.NPCScriptManager
import server.InventoryManipulator
import server.InventoryManipulator.Companion.addById
import server.InventoryManipulator.Companion.addFromDrop
import server.InventoryManipulator.Companion.removeById
import server.InventoryManipulator.Companion.removeFromSlot
import server.ItemInformationProvider.getEquipById
import server.ItemInformationProvider.getInventoryType
import server.ItemInformationProvider.getItemEffect
import server.ItemInformationProvider.randomizeStats
import server.life.LifeFactory
import server.life.MobSkillFactory
import server.maps.GameMap
import server.maps.MapObjectType
import server.quest.Quest
import tools.PacketCreator
import tools.packet.CharacterPacket
import tools.packet.GameplayPacket
import tools.packet.InteractPacket
import tools.packet.ItemPacket
import java.awt.Point

open class AbstractPlayerInteraction(open val c: Client) {
    fun canHold(itemId: Int) = (c.player?.getInventory(getInventoryType(itemId))?.getNextFreeSlot() ?: 0) > -1

    fun changeMusic(songName: String) = c.player?.map?.broadcastMessage(GameplayPacket.musicChange(songName))

    fun containsAreaInfo(area: Short, info: String) = c.player?.containsAreaInfo(area.toInt(), info) ?: false

    fun disableMiniMap() = c.announce(PacketCreator.disableMinimap())

    fun displayGuide(num :Int) = c.announce(GameplayPacket.showInfo("UI/tutorial.img/$num"))

    fun earnTitle(message: String) = c.announce(PacketCreator.earnTitleMessage(message))

    fun environmentChange(env: String, mode: Int) = c.player?.map?.broadcastMessage(GameplayPacket.environmentChange(env, mode))

    fun gainAndEquip(itemId: Int, slot: Byte) {
        c.player?.getInventory(InventoryType.EQUIPPED)?.getItem(slot)?.let {
            removeFromSlot(c, InventoryType.EQUIPPED, slot, it.quantity, false)
        }
        val newItem = getEquipById(itemId)
        newItem.position = slot
        val addFromDatabase = c.player?.getInventory(InventoryType.EQUIPPED)?.addFromDatabase(newItem)
        c.announce(CharacterPacket.modifyInventory(false, listOf(ModifyInventory(0, newItem))))
    }

    open fun gainItem(id: Int, quantity: Short) {
        gainItem(id, quantity, randomStats = false, showMessage = false)
    }

    open fun gainItem(id: Int, quantity: Short, show: Boolean) = gainItem(id, quantity, false, show)

    open fun gainItem(id: Int, show: Boolean) = gainItem(id, 1.toShort(), false, show)

    open fun gainItem(id: Int) = gainItem(id, 1.toShort(), randomStats = false, showMessage = false)

    private fun gainItem(id: Int, quantity: Short, randomStats: Boolean, showMessage: Boolean) {
        if (id in 5000000..5000100) {
            addById(c, id, 1.toShort(), null, createPet(id), -1)
        }
        if (quantity >= 0) {
            val item = getEquipById(id)
            if (!InventoryManipulator.checkSpace(c, id, quantity.toInt(), "")) {
                c.player?.dropMessage(
                    1,
                    "Your inventory is full. Please remove an item from your ${getInventoryType(id).name} inventory."
                )
                return
            }
            if (getInventoryType(id) == InventoryType.EQUIP && !ItemConstants.isRechargeable(item.itemId)) {
                item as Equip
                if (randomStats) {
                    addFromDrop(c, randomizeStats(item), false)
                } else {
                    addFromDrop(c, item, false)
                }
            } else {
                addById(c, id, quantity)
            }
        } else {
            removeById(c, getInventoryType(id), id, -quantity, fromDrop = true, consume = false)
        }
        if (showMessage) c.announce(ItemPacket.getShowItemGain(id, quantity, true))
    }

    fun getPlayer() = c.player

    fun getEventManager(event: String) = c.getChannelServer().eventSM.getEventManager(event)

    fun getGuild() = Server.getGuild(c.player?.guildId ?: -1)

    fun getMapId() = c.player?.map?.mapId

    fun getMobSkill(skill: Int, level: Int) = MobSkillFactory.getMobSkill(skill, level)

    open fun getParty() = c.player?.party

    fun getPlayerCount(mapId: Int) = c.getChannelServer().mapFactory.getMap(mapId).characters.size

    fun getQuestProgress(qid: Int) = c.player?.getQuest(Quest.getInstance(qid))?.getProgress(0)

    fun getQuestStatus(id: Int) = c.player?.getQuest(Quest.getInstance(id))?.status ?: QuestStatus.Status.NOT_STARTED

    fun getWarpMap(map: Int): GameMap {
        val event = c.player?.eventInstance
        return event?.getMapInstance(map) ?: c.getChannelServer().mapFactory.getMap(map)
    }

    fun getMap(map: Int) = getWarpMap(map)

    fun givePartyExp(amount: Int, party: List<Character>) {
        party.forEach { it.gainExp((amount * it.expRate), show = true, inChat = true, white = true) }
    }

    fun givePartyItems(id: Int, quantity: Short, party: List<Character>) {
        party.forEach {
            val c = it.client
            if (quantity >= 0) {
                addById(c, id, quantity)
            } else {
                removeById(c, getInventoryType(id), id, -quantity, fromDrop = true, consume = false)
            }
            c.announce(ItemPacket.getShowItemGain(id, quantity, true))
        }
    }

    fun guideHint(hint: Int) = c.announce(PacketCreator.guideHint(hint))

    fun guildMessage(type: Int, message: String) = getGuild()?.guildMessage(InteractPacket.serverNotice(type, message))

    open fun haveItem(itemId: Int) = haveItem(itemId, 1)

    private fun haveItem(itemId: Int, quantity: Int) = (c.player?.getItemQuantity(itemId, false) ?: 0) >= quantity

    fun isLeader() = getParty()?.leader == c.player?.mpc

    fun isQuestCompleted(quest: Int) = getQuestStatus(quest) == QuestStatus.Status.COMPLETED

    fun isQuestStarted(quest: Int) = getQuestStatus(quest) == QuestStatus.Status.STARTED

    fun lockUI() {
        c.announce(PacketCreator.disableUI(true))
        c.announce(PacketCreator.lockUI(true))
    }

    fun mapEffect(path: String) = c.announce(GameplayPacket.mapEffect(path))

    fun mapMessage(type: Int, message: String) = c.player?.map?.broadcastMessage(InteractPacket.serverNotice(type, message))

    fun mapSound(path: String) = c.announce(GameplayPacket.mapSound(path))

    fun message(message: String) = c.player?.message(message)

    fun openNpc(npcId: Int) {
        NPCScriptManager.dispose(c)
        NPCScriptManager.start(c, npcId, null, null)
    }

    fun openUI(ui: Byte) = c.announce(PacketCreator.openUI(ui))

    fun playerMessage(type: Int, message: String) = c.announce(InteractPacket.serverNotice(type, message))

    fun playSound(sound: String) = c.player?.map?.broadcastMessage(GameplayPacket.environmentChange(sound, 4))

    fun removeAll(id: Int) = removeAll(id, c)

    private fun removeAll(id: Int, c: Client) {
        val possessed = c.player?.getInventory(getInventoryType(id))?.countById(id) ?: return
        if (possessed > 0) {
            removeById(c, getInventoryType(id), id, possessed, fromDrop = true, consume = false)
            c.announce(ItemPacket.getShowItemGain(id, (-possessed).toShort(), true))
        }
    }

    fun removeEquipFromSlot(slot: Byte) {
        val tempItem = c.player?.getInventory(InventoryType.EQUIPPED)?.getItem(slot) ?: return
        removeFromSlot(c, InventoryType.EQUIPPED, slot, tempItem.quantity, false)
    }

    fun removeFromParty(id: Int, party: List<Character>) {
        party.forEach {
            val c = it.client
            val type = getInventoryType(id)
            val iv = c.player?.getInventory(type) ?: return@forEach
            val possessed = iv.countById(id)
            if (possessed > 0) {
                removeById(c, getInventoryType(id), id, possessed, fromDrop = true, consume = false)
                c.announce(ItemPacket.getShowItemGain(id, (-possessed).toShort(), true))
            }
        }
    }

    fun removeGuide() = c.announce(GameplayPacket.spawnGuide(false))

    open fun resetMap(mapId: Int) {
        val map = getMap(mapId)
        map.resetReactors()
        map.killAllMonsters()
        c.player?.position?.let {
            map.getMapObjectsInRange(it, Double.POSITIVE_INFINITY, listOf(MapObjectType.ITEM)).forEach { itt ->
                map.removeMapObject(itt)
                c.player?.id?.let { it1 -> GameplayPacket.removeItemFromMap(itt.objectId, 0, it1) }
                    ?.let { it2 -> map.broadcastMessage(it2) }
            }
        }
    }

    fun sendClock(c: Client, time: Int) = c.announce(PacketCreator.getClock(((time - System.currentTimeMillis()) / 1000).toInt()))

    fun showIntro(path: String) = c.announce(GameplayPacket.showIntro(path))

    fun showInfo(path: String) {
        c.announce(GameplayPacket.showInfo(path))
        c.announce(PacketCreator.enableActions())
    }

    fun showInfoText(message: String) = c.announce(GameplayPacket.showInfoText(message))

    fun showInstruction(message: String, width: Int, height: Int) {
        c.announce(PacketCreator.sendHint(message, width, height))
        c.announce(PacketCreator.enableActions())
    }

    fun spawnGuide() = c.announce(GameplayPacket.spawnGuide(true))

    fun spawnMonster(id: Int, x: Int, y: Int) {
        val monster = LifeFactory.getMonster(id)
        monster?.position = Point(x, y)
        monster?.let { c.player?.map?.spawnMonster(it, true) }
    }

    fun talkGuide(message: String) = c.announce(PacketCreator.talkGuide(message))

    fun teachSkill(skillId: Int, level: Byte, masterLevel: Byte, expiration: Int) {
        SkillFactory.getSkill(skillId)
            ?.let { c.player?.changeSkillLevel(it, level, masterLevel.toInt(), expiration = expiration.toLong()) }
    }

    fun timeLimit(s: Int) = c.player?.timeLimit(s)

    fun unlockUI() {
        c.announce(PacketCreator.disableUI(false))
        c.announce(PacketCreator.lockUI(false))
    }

    fun updateAreaInfo(area: Short, info: String) {
        c.player?.updateAreaInfo(area.toInt(), info)
        c.announce(PacketCreator.enableActions())
    }

    fun updateQuest(questId: Int, data: String) {
        val status = c.player?.getQuest(Quest.getInstance(questId)) ?: return
        status.status = QuestStatus.Status.STARTED
        status.setProgress(0, data)
        c.player?.updateQuest(status)
    }

    fun useItem(id: Int) {
        c.player?.let { getItemEffect(id)?.applyTo(it) }
        c.announce(ItemPacket.getItemMessage(id))
    }

    fun warp(map: Int) = warp(map, 0)

    fun warp(map: Int, portal: Int) = c.player?.changeMap(getWarpMap(map), getWarpMap(map).getPortal(portal))

    fun warp(map: Int, portal: String) {
        val warpMap = getWarpMap(map)
        c.player?.changeMap(warpMap, warpMap.getPortal(portal))
    }

    fun warpMap(map: Int) {
        c.player?.map?.characters?.forEach { it.changeMap(getWarpMap(map), getWarpMap(map).getPortal(0)) }
    }
}