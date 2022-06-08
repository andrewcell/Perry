package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.PacketCreator

class LeftKnockbackHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.announce(PacketCreator.leftKnockBack())
        c.announce(PacketCreator.enableActions())
    }
}