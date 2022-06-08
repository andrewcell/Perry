package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import server.maps.FieldLimit
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket

class TrockAddMapHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        val type = slea.readByte()
        val vip = slea.readByte().toInt() == 1
        when (type.toInt()) {
            0x00 -> {
                val mapId = slea.readInt()
                if (vip) {
                    chr.deleteFromVipTrockMaps(mapId)
                } else {
                    chr.deleteFromTrockMaps(mapId)
                }
                c.announce(CashPacket.trockRefreshMapList(chr, true, vip))
            }
            0x01 -> {
                if (!FieldLimit.CANNOTVIPROCK.check(chr.map.fieldLimit)) {
                    if (vip) {
                        chr.addVipTrockMap()
                    } else {
                        chr.addTrockMap()
                    }
                    c.announce(CashPacket.trockRefreshMapList(chr, false, vip))
                } else {
                    chr.message("You may not save this map.")
                }
            }
            else -> {}
        }
    }
}