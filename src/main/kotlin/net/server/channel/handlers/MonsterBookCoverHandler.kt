package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.MonsterBookPacket

class MonsterBookCoverHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val id = slea.readInt()
        if (id == 0 || id / 10000 == 238) {
            c.player?.bookCover = id
            c.announce(MonsterBookPacket.changeCover(id))
        }
    }
}