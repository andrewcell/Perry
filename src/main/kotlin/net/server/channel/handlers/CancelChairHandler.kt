package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class CancelChairHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val id = slea.readShort().toInt()
        c.player?.let { player ->
            if (id == -1) { // Cancel Chair
                c.player?.chair = 0
                c.announce(CharacterPacket.cancelChair(-1))
                c.player?.map?.broadcastMessage(player, PacketCreator.showChair(player.id, 0), false)
            } else { // Use In-Map Chair
                player.chair = id
                c.announce(CharacterPacket.cancelChair(id))
            }
        }
    }
}