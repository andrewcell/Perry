package net

import client.Client
import tools.data.input.SeekableLittleEndianAccessor

abstract class AbstractPacketHandler : PacketHandler {
    override fun validateState(c: Client) = c.loggedIn
}