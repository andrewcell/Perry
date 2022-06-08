package net.server.channel.handlers

import client.Client
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket
import java.awt.Point

class MoveSummonHandler : AbstractMovementPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val summonSkill = slea.readInt()
        val startPos = Point(slea.readShort().toInt(), slea.readShort().toInt())
        val res = parseMovement(slea)
        val player = c.player
        val summon = player?.summons?.get(summonSkill)
        if (summon != null) {
            updatePosition(res, summon, 0)
            player.map.broadcastMessage(player, GameplayPacket.moveSummon(player.id, summonSkill, startPos, res), summon.position)
        }
    }
}