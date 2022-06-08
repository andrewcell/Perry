package net.server.channel.handlers

import client.Client
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket

class MovePlayerHandler : AbstractMovementPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        slea.skip(5)
        val res = parseMovement(slea)
        val player = c.player
        if (res != null && player != null) {
            if (slea.available().toInt() != 10) return
            updatePosition(res, player, 0)
            player.map.movePlayer(player, player.position)
            if (player.hidden) {
                c.player?.map?.broadcastGMMessage(player, GameplayPacket.movePlayer(player.id, res), false)
            } else {
                c.player?.map?.broadcastMessage(player, GameplayPacket.movePlayer(player.id, res), false)
            }
        }
    }
}