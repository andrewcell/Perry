package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType.Companion.getByType
import server.InventoryManipulator.Companion.unEquip
import server.InventoryManipulator.Companion.equip
import server.InventoryManipulator.Companion.drop
import server.InventoryManipulator.Companion.move
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class ItemMoveHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val type = getByType(slea.readByte())
        val src = slea.readShort().toByte()
        val action = slea.readShort().toByte()
        val quantity = slea.readShort()
        when {
            src < 0 && action > 0 -> unEquip(c, src, action)
            action < 0 -> equip(c, src, action)
            action.toInt() == 0 -> drop(c, type, src, quantity)
            else -> move(c, type, src, action)
        }
    }
}