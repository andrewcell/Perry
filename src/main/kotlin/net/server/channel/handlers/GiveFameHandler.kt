package net.server.channel.handlers

import client.Character
import client.Character.FameStats
import client.CharacterStat
import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket
import kotlin.math.abs

class GiveFameHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val target = c.player?.map?.mapObjects?.get(slea.readInt()) as? Character
        val mode = slea.readByte().toInt()
        val fameChange = 2 * mode - 1
        val player = c.player ?: return
        if (target == null || target == player || player.level < 15) return
        when (player.canGiveFame(target)) {
            FameStats.OK -> {
                if (abs(target.fame + fameChange) < 30001) {
                    target.addFame(fameChange)
                    target.updateSingleStat(CharacterStat.FAME, target.fame, false)
                }
                if (!player.isGM()) {
                    player.hasGivenFame(target)
                }
                c.announce(InteractPacket.giveFameResponse(mode, target.name, target.fame))
                target.client.announce(InteractPacket.receiveFame(mode, player.name))
            }
            FameStats.NOT_TODAY -> c.announce(InteractPacket.giveFameErrorResponse(3))
            FameStats.NOT_THIS_MONTH -> c.announce(InteractPacket.giveFameErrorResponse(4))
        }
    }
}