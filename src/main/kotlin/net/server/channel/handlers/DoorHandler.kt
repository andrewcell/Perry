package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import server.maps.Door

class DoorHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val oid = slea.readInt()
        val mode = slea.readByte().toInt() == 0 // specifies if back warp or not, 1 town to target, 0 target to town
        c.player?.let { player ->
            for (obj in player.map.mapObjects.values) {
                if (obj is Door) {
                    if (obj.owner.id == oid) {
                        obj.warp(player, mode)
                        return
                    }
                }
            }
        }
    }
}