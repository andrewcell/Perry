package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket

class AdminChatHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        if (c.player?.isGM() != true) { //if ( (signed int)CWvsContext::GetAdminLevel((void *)v294) > 2 )
            return
        }
        val mode = slea.readByte()
        //not saving slides...
        val packet = InteractPacket.serverNotice(
            slea.readByte().toInt(),
            slea.readGameASCIIString()
        )
        when (mode.toInt()) {
            0 -> c.getWorldServer().broadcastPacket(packet)
            1 -> c.getChannelServer().broadcastPacket(packet)
            2 -> c.player?.map?.broadcastMessage(packet)
        }
    }
}