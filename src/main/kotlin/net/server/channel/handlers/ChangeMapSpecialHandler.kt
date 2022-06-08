package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor

class ChangeMapSpecialHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val startWp = slea.readGameASCIIString()
        val portal = c.player?.map?.getPortal(startWp)
        if (portal == null || (c.player?.portalDelay ?: 0) > System.currentTimeMillis() || c.player?.blockedPortals?.contains(portal.scriptName) == true
        ) {
            c.announce(PacketCreator.enableActions())
            return
        }
        portal.enterPortal(c)
    }
}