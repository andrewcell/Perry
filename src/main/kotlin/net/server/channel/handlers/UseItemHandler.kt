package net.server.channel.handlers

import client.Client
import client.Disease
import client.inventory.InventoryType
import net.AbstractPacketHandler
import server.InventoryManipulator
import server.ItemInformationProvider
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor

class UseItemHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        if (c.player?.isAlive() != true) {
            c.announce(PacketCreator.enableActions())
            return
        }
        val slot = slea.readShort().toByte()
        val itemId = slea.readInt()
        val toUse = c.player?.getInventory(InventoryType.USE)?.getItem(slot)
        if (toUse != null && toUse.quantity > 0 && toUse.itemId == itemId) {
            when (itemId) {
                2022178, 2022433, 2050004 -> {
                    c.player?.dispelDeBuffs()
                    remove(c, slot)
                    return
                }
                2050003 -> {
                    c.player?.dispelDeBuff(Disease.SEAL)
                    remove(c, slot)
                    return
                }
                else -> {
                    c.player?.let { p ->
                        if (isTownScroll(toUse.itemId)) {
                            ItemInformationProvider.getItemEffect(toUse.itemId)?.let {
                                if (it.applyTo(p)) remove(c, slot)
                            }
                            c.announce(PacketCreator.enableActions())
                            return
                        }
                        remove(c, slot)
                        ItemInformationProvider.getItemEffect(toUse.itemId)?.applyTo(p)
                        p.checkBerserk()
                    }
                }
            }
        }
    }

    fun remove(c: Client, slot: Byte) {
        InventoryManipulator.removeFromSlot(c, InventoryType.USE, slot, 1, fromDrop = false, consume = false)
    }

    private fun isTownScroll(itemId: Int) = itemId in 2030000..2030020
}
