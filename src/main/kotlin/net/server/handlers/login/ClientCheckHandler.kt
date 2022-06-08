package net.server.handlers.login

import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import client.Client

class ClientCheckHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val type = slea.readByte().toInt()
        if (type == 1) c.isConnector = true
    }
}