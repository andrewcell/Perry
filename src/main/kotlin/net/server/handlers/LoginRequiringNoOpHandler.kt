package net.server.handlers

import client.Client
import net.PacketHandler
import tools.data.input.SeekableLittleEndianAccessor

object LoginRequiringNoOpHandler : PacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) { }

    override fun validateState(c: Client) = c.loggedIn
}