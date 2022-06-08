package net.server.handlers.login

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class DeleteCharHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        //val secondPasswordClient = slea.readInt().toString() + ""
        //val socialNumber = slea.readInt()
        val cid = slea.readInt()
        c.announce(CharacterPacket.deleteCharResponse(cid, 0))
        c.deleteCharacter(cid)
    }
}