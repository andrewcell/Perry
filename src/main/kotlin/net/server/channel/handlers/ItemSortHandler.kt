package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType.Companion.getByType
import net.AbstractPacketHandler
import server.InventoryManipulator.Companion.move
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class ItemSortHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        chr.autoban.setTimestamp(2, slea.readInt())
        val inv = slea.readByte()
        if (inv in 1..5) {
            var sorted = false
            val pInvType = getByType(inv)
            val pInv = chr.getInventory(pInvType)
            while (!sorted) {
                val freeSlot = pInv!!.getNextFreeSlot()
                if (freeSlot.toInt() != -1) {
                    var itemSlot: Byte = -1
                    for (i in freeSlot + 1..pInv.slotLimit) {
                        if (pInv.getItem(i.toByte()) != null) {
                            itemSlot = i.toByte()
                            break
                        }
                    }
                    if (itemSlot in 1..96) {
                        move(c, pInvType, itemSlot, freeSlot)
                    } else {
                        sorted = true
                    }
                } else {
                    sorted = true
                }
            }
            c.announce(CharacterPacket.finishedSort(inv.toInt()))
        }
        c.announce(PacketCreator.enableActions())
    }
}