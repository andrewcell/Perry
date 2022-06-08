package net.server.channel.handlers

import client.BuffStat
import client.Client
import client.inventory.InventoryType
import net.AbstractPacketHandler
import net.server.Server
import server.Trade
import server.maps.FieldLimit
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.LoginPacket
import java.net.InetAddress

class ChangeChannelHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val channel = slea.readByte() + 1
        val chr = c.player
        if (chr?.banned == true) {
            c.disconnect(shutdown = false, cashShop = false)
            return
        }
        chr?.let { player ->
            when (player.mapId) {
                190000000, 190000001, 190000002, 191000000, 191000001, 192000000, 192000001, 195000000, 195010000, 195020000, 195030000, 196000000, 196010000, 197000000, 197010000 -> {
                    chr.dropMessage(1, "이 곳에서는 할 수 없는 일입니다.")
                    c.announce(PacketCreator.enableActions())
                    return
                }
                else -> {}
            }
            if (!player.isAlive() || FieldLimit.CHANGECHANNEL.check(player.map.fieldLimit)) {
                c.announce(PacketCreator.enableActions())
                return
            }
            val socket = Server.getIp(c.world, channel)?.split(":")
            if (chr.trade != null) Trade.cancelTrade(player)
            val merchant = player.hiredMerchant
            if (merchant != null) {
                if (merchant.isOwner(player)) {
                    merchant.open = true
                } else {
                    merchant.removeVisitor(player)
                }
            }
            Server.buffStorage.addBuffsToStorage(player.id, player.getAllBuffs())
            player.cancelBuffEffects()
            player.cancelMagicDoor()
            player.saveCoolDowns()
            if (player.getBuffedValue(BuffStat.PUPPET) != null) {
                player.cancelEffectFromBuffStat(BuffStat.PUPPET)
            }
            if (player.getBuffedValue(BuffStat.COMBO) != null) {
                player.cancelEffectFromBuffStat(BuffStat.COMBO)
            }
            player.getInventory(InventoryType.EQUIPPED)?.checked = false
            player.map.removePlayer(player)
            player.client.getChannelServer().removePlayer(player)
            player.saveToDatabase()
            player.client.updateLoginState(Client.LOGIN_SERVER_TRANSITION)
            player.channelCheck = true
            socket?.let { c.announce(LoginPacket.getChannelChange(InetAddress.getByName(it[0]), it[1].toInt())) }
        }
    }
}