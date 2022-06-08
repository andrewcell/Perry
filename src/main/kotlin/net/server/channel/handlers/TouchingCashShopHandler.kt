package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket

class TouchingCashShopHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.player?.let { c.announce(CashPacket.showCash(it)) }
        c.announce(CashPacket.enableCSUse())
        c.announce(CashPacket.showLocker(c))
        c.player?.let { c.announce(CashPacket.showWishList(it, false)) }
        c.announce(PacketCreator.enableActions())
    }
}