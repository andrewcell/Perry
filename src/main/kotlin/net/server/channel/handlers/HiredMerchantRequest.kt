package net.server.channel.handlers

import client.Client
import client.inventory.ItemFactory
import net.AbstractPacketHandler
import server.maps.MapObjectType
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket

class HiredMerchantRequest : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        if (chr.map.getMapObjectsInRange(chr.position, 23000.0, listOf(MapObjectType.HIRED_MERCHANT)).isEmpty() && chr.mapId > 910000000 && chr.mapId < 910000023) {
            if (!chr.hasMerchant) {
                if (ItemFactory.MERCHANT.loadItems(chr.id, false).isEmpty() && chr.merchantMeso == 0) {
                    c.announce(CashPacket.hiredMerchantBox())
                } else {
                    chr.announce(PacketCreator.retrieveFirstMessage())
                }
            } else {
                chr.dropMessage(1, "이미 사용중인 상점이 존재합니다.")
            }
        } else {
            chr.dropMessage(1, "이 곳에선 상점을 개설할 수 없습니다.")
        }
    }
}