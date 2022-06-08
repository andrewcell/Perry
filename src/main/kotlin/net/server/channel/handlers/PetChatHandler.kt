package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket

class PetChatHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        //val petId = 0 //slea.readInt();
        val act = slea.readShort().toInt()
        val text = slea.readGameASCIIString()
        c.player?.let {
            it.map.broadcastMessage(
                it,
                CashPacket.petChat(it.id, 0.toByte(), act, text),
                true
            )
        }
    }
}