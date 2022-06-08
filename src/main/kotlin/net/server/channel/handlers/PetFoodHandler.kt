package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import constants.ExpTable
import net.AbstractPacketHandler
import server.InventoryManipulator
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket
import kotlin.random.Random

class PetFoodHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player
        val pet = chr?.pet
        if (pet == null) {
            c.announce(PacketCreator.enableActions())
            return
        }
        var previousFullness = 100
        var slot = 0
        if (pet.fullness < previousFullness) previousFullness = pet.fullness
        val pos = slea.readShort().toByte()
        val itemId = slea.readInt()
        val use = chr.getInventory(InventoryType.USE)?.getItem(pos)
        if (use == null || (itemId / 10000) != 212 || use.itemId != itemId) return
        val gainCloseness = Random.nextInt(101) > 50
        if (pet.fullness < 100) {
            var newFullness = pet.fullness + 30
            if (newFullness > 100) newFullness = 100
            pet.fullness = newFullness
            if (gainCloseness && pet.closeness < 30000) {
                var newCloseness = pet.closeness + 1
                if (newCloseness > 30000) newCloseness = 30000
                pet.closeness = newCloseness
                if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.level.toInt())) {
                    pet.level = (pet.level + 1).toByte()
                    c.announce(CashPacket.showOwnPetLevelUp())
                    chr.map.broadcastMessage(CashPacket.showPetLevelUp(chr))
                }
            }
            chr.map.broadcastMessage(chr, CashPacket.petCommandResponse(chr.id, slot.toByte(), 1, true), true)
        } else {
            if (gainCloseness) {
                var newCloseness = pet.closeness - 1
                if (newCloseness < 0) newCloseness = 0
                pet.closeness = newCloseness
                if (pet.level > 1 && newCloseness < ExpTable.getClosenessNeededForLevel(pet.level.toInt()))
                    pet.level = (pet.level - 1).toByte()
            }
            chr.map.broadcastMessage(chr, CashPacket.petCommandResponse(chr.id, slot.toByte(), 0, false), true)
        }
        InventoryManipulator.removeFromSlot(c, InventoryType.USE, pos, 1, fromDrop = false, consume = false)
        val petZ = chr.getInventory(InventoryType.CASH)?.getItem(pet.position) ?: return
        chr.forceUpdateItem(petZ)

    }
}