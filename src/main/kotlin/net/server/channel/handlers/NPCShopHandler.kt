package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import server.ItemInformationProvider
import tools.data.input.SeekableLittleEndianAccessor

class NPCShopHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val mode = slea.readByte().toInt()
        when (mode) {
            0, 1 -> { // Buy, Sell
                val slot = slea.readShort()
                val itemId = slea.readInt()
                val quantity = slea.readShort()
                if (mode == 0)
                    c.player?.shop?.buy(c, slot, itemId, quantity)
                else
                    c.player?.shop?.sell(c, ItemInformationProvider.getInventoryType(itemId), slot, quantity)
            }
            2 -> { // Recharge
                val slot = slea.readShort()
                c.player?.shop?.recharge(c, slot.toByte())
            }
            3 -> { // Leaving
                c.player?.shop = null
                c.player?.conversation = 0
            }
        }
    }
}