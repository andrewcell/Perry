package net.server.handlers.login

import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import client.Client

class LoginSuccessHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val type = slea.readByte().toInt()
        // 10 = Maintenance mode
        // 2 = Server on
        if (type == 2) {
            c.isConnector = true
        }
    }
}