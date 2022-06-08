package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.ItemPacket

class UseDeathItemHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val itemId = slea.readInt()
        c.player?.let { player ->
            player.itemEffect = itemId
            c.announce(ItemPacket.itemEffect(player.id, itemId))
        }
    }
}