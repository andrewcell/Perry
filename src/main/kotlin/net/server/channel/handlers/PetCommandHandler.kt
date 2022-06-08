package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import client.inventory.PetDataFactory
import constants.ExpTable
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket
import kotlin.random.Random

class PetCommandHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player
        val pet = c.player?.pet ?: return
        slea.readByte()
        val command = slea.readByte()
        val petCommand = PetDataFactory.getPetCommand(pet.itemId, command.toInt()) ?: return
        var success = false
        if (Random.nextInt(101) <= petCommand.probability) {
            success = true
            if (pet.closeness < 30000) {
                var newCloseness = pet.closeness + petCommand.increase
                if (newCloseness > 30000) newCloseness = 30000
                pet.closeness = newCloseness
                if (newCloseness >= ExpTable.getClosenessNeededForLevel(pet.level.toInt())) {
                    pet.level = (pet.level + 1).toByte()
                    c.announce(CashPacket.showOwnPetLevelUp())
                    chr?.map?.broadcastMessage(CashPacket.showPetLevelUp(c.player))
                }
                val petZ = chr?.getInventory(InventoryType.CASH)?.getItem(pet.position) ?: return
                chr.forceUpdateItem(petZ)
            }
        }
        c.player?.let { chr?.map?.broadcastMessage(it, CashPacket.petCommandResponse(chr.id, 0, command.toInt(), success), true) }
    }
}