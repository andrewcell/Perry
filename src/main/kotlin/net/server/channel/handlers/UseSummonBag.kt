package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import net.AbstractPacketHandler
import server.InventoryManipulator.Companion.removeFromSlot
import server.ItemInformationProvider.getSummonMobs
import server.life.LifeFactory.Companion.getMonster
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import kotlin.random.Random

class UseSummonBag : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        //[4A 00][6C 4C F2 02][02 00][63 0B 20 00]
        c.player?.let { player ->
            if (!player.isAlive()) {
                c.announce(PacketCreator.enableActions())
                return
            }
            slea.readInt()
            val slot = slea.readShort().toByte()
            val itemId = slea.readInt()
            val toUse = player.getInventory(InventoryType.USE)?.getItem(slot)
            if (toUse != null && toUse.quantity > 0 && toUse.itemId == itemId) {
                removeFromSlot(c, InventoryType.USE, slot, 1.toShort(), false, false)
                val toSpawn = getSummonMobs(itemId)
                for (toSpawnChild in toSpawn) {
                    if (Random.nextInt(101) <= toSpawnChild[1]) {
                        player.map.spawnMonsterOnGroundBelow(getMonster(toSpawnChild[0])!!, player.position)
                    }
                }
            }
            c.announce(PacketCreator.enableActions())
        }
    }
}