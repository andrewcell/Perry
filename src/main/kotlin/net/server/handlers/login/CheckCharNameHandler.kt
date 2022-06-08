package net.server.handlers.login

import client.Character
import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class CheckCharNameHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val name = slea.readGameASCIIString()
        c.announce(CharacterPacket.charNameResponse(name, !Character.checkNameAvailable(name)))
    }
}