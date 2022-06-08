package net.server.channel.handlers

import client.Client
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket

class MovePetHandler : AbstractMovementPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        slea.readInt()
        val res = parseMovement(slea)
        if (res.isEmpty()) return
        c.player?.let { player ->
            player.pet?.updatePosition(res)
            player.map.broadcastMessage(player, GameplayPacket.movePet(player.id, 0, res), false)
        }
    }
}