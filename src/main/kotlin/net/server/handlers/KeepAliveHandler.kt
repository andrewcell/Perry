package net.server.handlers

import client.Client
import net.PacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class KeepAliveHandler : PacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) = c.pongReceived()

    override fun validateState(c: Client) = true
}