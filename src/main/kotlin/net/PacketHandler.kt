package net

import client.Client
import tools.data.input.SeekableLittleEndianAccessor

interface PacketHandler {
    fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client)
    fun validateState(c: Client): Boolean
}