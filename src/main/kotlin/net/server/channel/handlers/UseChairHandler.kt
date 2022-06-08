package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import net.AbstractPacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor

class UseChairHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val itemId = slea.readInt()
        c.player?.let { player ->
            if (player.getInventory(InventoryType.SETUP)?.findById(itemId) == null) {
                return
            }
            player.chair = itemId
            player.map.broadcastMessage(player, PacketCreator.showChair(player.id, itemId), false)
            c.announce(PacketCreator.enableActions())
        }
    }
}