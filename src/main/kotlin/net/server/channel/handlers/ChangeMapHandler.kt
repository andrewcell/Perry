package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import mu.KLogging
import net.AbstractPacketHandler
import server.InventoryManipulator
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket
import tools.packet.LoginPacket
import java.net.InetAddress

class ChangeMapHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.player?.let { chr ->
            if (chr.banned) return
            if (slea.available() == 0L) { // CashShop
                val socket = c.getChannelServer().ip.split(":")
                chr.saveToDatabase()
                c.getChannelServer().addPlayer(chr)
                c.updateLoginState(Client.LOGIN_SERVER_TRANSITION)
                c.announce(LoginPacket.getChannelChange(InetAddress.getByName(socket[0]), socket[1].toInt()))
            } else {
                slea.readByte() // 1 from dying 0 regular portals
                val targetId = slea.readInt()
                try {
                    val startWp = slea.readGameASCIIString()
                    val portal = chr.map.getPortal(startWp)
                    val wheel = slea.readShort() > 0
                    when {
                        targetId != 1 && !chr.isAlive() -> {
                            val executeStandardPath = chr.eventInstance?.revivePlayer(chr) ?: true
                            if (executeStandardPath) {
                                var to = chr.map
                                if (wheel && chr.getItemQuantity(5510000, false) > 0) {
                                    InventoryManipulator.removeById(c, InventoryType.CASH, 5510000, 1,
                                        fromDrop = true,
                                        consume = false
                                    )
                                    chr.announce(GameplayPacket.showWheelsLeft(chr.getItemQuantity(5510000, false)))
                                } else {
                                    chr.cancelAllBuffs(false)
                                    to = chr.map.getReturnMap()
                                }
                                chr.setHpNormal(50)
                                chr.changeMap(to)
                            }
                        }
                        targetId != 1 && chr.isGM() -> {
                            val to = c.getChannelServer().mapFactory.getMap(targetId)
                            chr.changeMap(to)
                        }
                        targetId != 1 && !chr.isGM() -> {
                            val divided = chr.mapId / 100
                            var warp = false
                            when (divided) {
                                0 -> if (targetId == 10000) warp = true
                                20100 -> if (targetId == 104000000) {
                                    c.announce(PacketCreator.lockUI(false))
                                    c.announce(PacketCreator.disableUI(false))
                                    warp = true
                                }
                            }
                            if (divided / 10 == 1020) if (targetId == 1020000) warp = true // Adventurer movie clip intro
                            if (warp) {
                                val to = c.getChannelServer().mapFactory.getMap(targetId)
                                chr.changeMap(to)
                            }
                        }
                    }
                    if (portal != null && !portal.portalStatus) {
                        c.announce(PacketCreator.blockedMessage(1))
                        c.announce(PacketCreator.enableActions())
                        return
                    }
                    if (chr.mapId == 109040004) chr.fitness?.resetTimes()
                    if (chr.mapId == 109030003 || chr.mapId == 109030103) chr.ola?.resetTimes()
                    if (portal != null) {
                        if (portal.targetMapId == 193000000) chr.timeSet = -1
                        portal.enterPortal(c)
                        if (chr.timeSet > 0) {
                            chr.announce(PacketCreator.getClock(chr.timeSet))
                        }
                    } else {
                        c.announce(PacketCreator.enableActions())
                    }
                    chr.setRates()
                } catch (e: Exception) {
                    logger.error(e) { "Error caused when handling change map. target map id: $targetId" }
                }
            }
        }
    }

    companion object : KLogging()
}