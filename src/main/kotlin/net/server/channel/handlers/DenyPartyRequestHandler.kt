package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket

class DenyPartyRequestHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        slea.readByte()
        val cFrom = c.getChannelServer().players.getCharacterByName(slea.readGameASCIIString())
        cFrom?.client?.announce(InteractPacket.partyStatusMessage(21, c.player?.name))
    }
}