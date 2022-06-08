package net.server.handlers.login

import client.Client
import net.AbstractPacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor

class ReLogRequestHandler : AbstractPacketHandler() {
    override fun validateState(c: Client): Boolean = !c.loggedIn

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.announce(PacketCreator.getRelogResponse())
    }
}