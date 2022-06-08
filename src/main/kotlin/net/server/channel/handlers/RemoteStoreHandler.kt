package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import net.server.Server
import server.maps.HiredMerchant
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket

class RemoteStoreHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        val hm = getMerchant(c)
        if (chr.hasMerchant && hm != null) {
            if (hm.channel == chr.client.channel) {
                hm.open = false
                hm.removeAllVisitors("")
                chr.hiredMerchant = hm
                chr.announce(CashPacket.getHiredMerchant(chr, hm, false))
            } else {
                c.announce(PacketCreator.remoteChannelChange((hm.channel - 1).toByte()))
            }
        } else {
            chr.dropMessage(1, "You don't have a Merchant open")
        }
        c.announce(PacketCreator.enableActions())
    }

    fun getMerchant(c: Client): HiredMerchant? {
        if (c.player?.hasMerchant == true) {
            Server.getChannelsFromWorld(c.world).forEach {
                val m = it.hiredMerchants[c.player?.id]
                if (m != null) return m
            }
        }
        return null
    }
}