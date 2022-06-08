package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import net.AbstractPacketHandler
import server.InventoryManipulator
import server.ItemInformationProvider
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket

class PetAutoPotHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        if (c.player?.isAlive() == false) {
            c.announce(PacketCreator.enableActions())
            return
        }
        slea.readByte()
        val slot = slea.readShort().toByte()
        val itemId = slea.readInt()
        val toUse = c.player?.getInventory(InventoryType.USE)?.getItem(slot)
        if (toUse != null && toUse.quantity > 0) {
            if (toUse.itemId != itemId) {
                c.announce(PacketCreator.enableActions())
                return
            }
            InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, 1, fromDrop = false, consume = false)
            val stat = ItemInformationProvider.getItemEffect(toUse.itemId)
            c.player?.let { stat?.applyTo(it) }
            if ((stat?.hp ?: 0) > 0) {
                c.player?.petAutoHp = itemId
                c.announce(CashPacket.sendPetAutoHpPot(itemId))
            }
        }
    }
}