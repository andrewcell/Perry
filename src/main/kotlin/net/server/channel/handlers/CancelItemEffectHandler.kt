package net.server.channel.handlers

import server.ItemInformationProvider.noCancelMouse
import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class CancelItemEffectHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val itemId = -slea.readInt()
        if (noCancelMouse(itemId)) {
            return
        }
        c.player?.cancelEffect(itemId)
    }
}