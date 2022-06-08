package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.ItemPacket

class UseItemEffectHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val itemId = slea.readInt()
        c.player?.let { player ->
            val toUse = if (itemId == 429001 || itemId == 4290000) {
                player.getInventory(InventoryType.ETC)?.findById(itemId)
            } else player.getInventory(InventoryType.CASH)?.findById(itemId)
            if (toUse == null || toUse.quantity < 1) {
                if (itemId != 0) return
            }
            player.itemEffect = itemId
            player.map.broadcastMessage(player, ItemPacket.itemEffect(player.id, itemId), false)
        }
    }
}
