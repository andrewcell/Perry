package scripting.reactor

import client.Client
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.Item
import scripting.AbstractPlayerInteraction
import server.life.LifeFactory
import server.life.LifeFactory.Companion.getMonster
import server.maps.Reactor
import server.maps.ReactorDropEntry
import tools.packet.NpcPacket
import java.awt.Point
import java.lang.Math.random
import server.ItemInformationProvider as ii

class ReactorActionManager(override val c: Client, val reactor: Reactor) : AbstractPlayerInteraction(c) {
    fun dropItems(meso: Boolean = false, mesoChance: Int = 0, minMeso: Int = 0, maxMeso: Int = 0, minItems: Int = 0) {
        val chances = getDropChances()
        val items = mutableListOf<ReactorDropEntry>()
        var numItems = 0
        if (meso && random() < (1 / mesoChance.toDouble())) {
            items.add(ReactorDropEntry(0, mesoChance, -1))
        }
        chances.forEach {
            if (random() < (1 / it.change)) {
                numItems++
                items.add(it)
            }
        }
        while (items.size < minItems) {
            items.add(ReactorDropEntry(0, mesoChance, -1))
            numItems++
        }
        items.shuffle()
        val dropPos = reactor.position
        dropPos.x -= (12 * numItems)
        items.forEach {
            if (it.itemId == 0) {
                val range = maxMeso - minMeso
                val displayDrop = (random() * range) + minMeso
                val mesoDrop = (displayDrop * c.getWorldServer().mesoRate)
                reactor.map?.spawnMesoDrop(mesoDrop.toInt(), dropPos, reactor, c.player, false, 0)
            } else {
                val drop = if (ii.getInventoryType(it.itemId) != InventoryType.EQUIP) {
                    Item(it.itemId, 0, 1, -1)
                } else {
                    ii.randomizeStats(ii.getEquipById(it.itemId) as Equip)
                }
                if (it.questId < 1) {
                    reactor.map?.spawnItemDrop(reactor, getPlayer(), drop, dropPos, ffaDrop = false, playerDrop = true)
                } else {
                    if (getPlayer()?.getQuestStatus(it.questId) == 1 && getPlayer()?.needQuestItem(it.questId, it.itemId) == true) {
                        getPlayer()?.let { it1 ->
                            reactor.map?.spawnQuestItemDrop(reactor, it1, drop, dropPos,
                                ffaDrop = false,
                                playerDrop = true,
                                questId = it.questId
                            )
                        }
                    }
                }
            }
            dropPos.x += 25
        }
    }

    private fun getDropChances() = ReactorScriptManager.getDrops(reactor.rid)

    private fun getPosition(): Point {
        val pos = reactor.position
        pos.y -= 10
        return pos
    }

    fun spawnNpc(npcId: Int, pos: Point = getPosition()) {
        val npc = LifeFactory.getNpc(npcId)
        npc.position = pos
        npc.cy = pos.y
        npc.rx0 = pos.x + 50
        npc.rx1 = pos.x - 50
        npc.fh = reactor.map?.footholds?.findBelow(pos)?.id
        reactor.map?.addMapObject(npc)
        reactor.map?.broadcastMessage(NpcPacket.spawnNpc(npc))
    }

    fun spawnMonster(id: Int, qty: Int, pos: Point) {
        for (i in 0 until qty) {
            reactor.map?.spawnMonsterOnGroundBelow(getMonster(id)!!, pos)
        }
    }
}