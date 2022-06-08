package net.server.channel.handlers

import client.Character
import client.Client
import net.AbstractPacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class CharInfoRequestHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        //slea.readInt();
        val cid = slea.readInt()
        val player = c.player?.map?.mapObjects?.get(cid) as? Character?
        if (player?.hidden == true && c.player?.isGM() != true) return
        if (player == null) {
            c.announce(PacketCreator.enableActions())
            return
        }
        c.announce(CharacterPacket.charInfo(player, c.player?.id == cid))
    }
}