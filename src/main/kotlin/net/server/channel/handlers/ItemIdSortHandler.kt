package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType.Companion.getByType
import client.inventory.Item
import net.AbstractPacketHandler
import server.InventoryManipulator.Companion.addFromDrop
import server.InventoryManipulator.Companion.removeById
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class ItemIdSortHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        chr.autoban.setTimestamp(4, slea.readInt())
        val invId = slea.readByte()
        if (invId < 0 || invId > 5) {
            c.disconnect(shutdown = false, cashShop = false)
            return
        }
        val inv = chr.getInventory(getByType(invId)) ?: return
        val itemArray = mutableListOf<Item>()
        val it: Iterator<Item> = inv.iterator()
        while (it.hasNext()) {
            itemArray.add(it.next().copy())
        }
        itemArray.sort()
        for (item in itemArray) {
            removeById(c, getByType(invId), item.itemId, item.quantity.toInt(), fromDrop = false, consume = false)
        }
        for (i in itemArray) {
            addFromDrop(c, i, false)
        }
        c.announce(CharacterPacket.finishedSort2(invId.toInt()))
    }
}