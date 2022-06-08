package net.server.channel.handlers

import client.Client
import mu.KLogging
import net.AbstractPacketHandler
import net.server.Server.buffStorage
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket

class EnterCashShopHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val mc = c.player ?: return
        try {
            if (mc.cashShop?.opened == true) return
            when (mc.mapId) {
                190000000, 190000001, 190000002, 191000000, 191000001, 192000000, 192000001, 195000000, 195010000, 195020000, 195030000, 196000000, 196010000, 197000000, 197010000 -> {
                    mc.dropMessage(1, "이 곳에서는 할 수 없는 일입니다.")
                    c.announce(PacketCreator.enableActions())
                    return
                }
            }
            buffStorage.addBuffsToStorage(mc.id, mc.getAllBuffs())
            mc.cancelBuffEffects()
            mc.cancelExpirationTask()
            c.announce(CashPacket.openCashShop(c))
            c.announce(CashPacket.showCash(mc))
            c.announce(CashPacket.enableCSUse())
            c.announce(CashPacket.showLocker(c))
            c.announce(CashPacket.showWishList(mc, false))
            c.announce(PacketCreator.enableActions())
            mc.saveToDatabase()
            mc.cashShop?.opened = true
            mc.map.removePlayer(mc)
        } catch (e: Exception) {
            logger.error(e) { "Error caused when handling EnterCashShop. Player: ${mc.name}" }
        }
    }

    companion object : KLogging()
}