package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class PetExcludeItemsHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        slea.readLong()
        val amount = slea.readByte()
        for (i in 0 until amount) {
            c.player?.addExcluded(slea.readInt())
        }
    }
}