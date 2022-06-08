package net.server.channel.handlers

import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import client.Client

class ReactorHitHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val oid = slea.readInt()
        val charPos = slea.readInt()
        val stance = slea.readShort()
        val skillId = 0
        val reactor = c.player?.map?.getReactorByOid(oid)
        if (reactor != null && reactor.alive) {
            reactor.hitReactor(c, charPos, stance, skillId)
        }
    }
}