package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GuildPacket

class DenyGuildRequestHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        slea.readByte()
        val cFrom = c.getChannelServer().players.getCharacterByName(slea.readGameASCIIString())
        cFrom?.client?.announce(GuildPacket.denyGuildInvitation(c.player?.name.toString()))
    }
}