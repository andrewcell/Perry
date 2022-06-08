package net.server.handlers

import client.Client
import net.PacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor

class CustomPacketHandler : PacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        if (slea.available() > 0 && c.gmLevel == 4) {
            c.announce(PacketCreator.customPacket(slea.read(slea.available().toInt())))
        }
    }

    override fun validateState(c: Client) = true
}