package net.server.channel.handlers

import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import client.Client
import scripting.reactor.ReactorScriptManager

class TouchReactorHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val oid = slea.readInt()
        val reactor = c.player?.map?.getReactorByOid(oid)
        if (reactor != null) {
            if (slea.readByte().toInt() != 0) {
                ReactorScriptManager.touch(c, reactor)
            } else {
                ReactorScriptManager.untouch(c, reactor)
            }
        }
    }
}