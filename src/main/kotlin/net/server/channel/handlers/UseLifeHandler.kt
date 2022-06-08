package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class UseLifeHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.announce(CharacterPacket.charNameResponse(slea.readGameASCIIString(), false))
    }
}