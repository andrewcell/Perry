package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.PacketCreator

class MesoDropHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.player?.let { chr ->
            if (!chr.isAlive()) {
                c.announce(PacketCreator.enableActions())
                return
            }
            val meso = slea.readInt()
            if (meso <= chr.meso.get() && meso > 9 && meso < 50001) {
                chr.gainMeso(-meso, show = false, enableActions = true, inChat = false)
                chr.map.spawnMesoDrop(meso, chr.position, chr, chr, true, 2.toByte())
            }
        }
    }
}