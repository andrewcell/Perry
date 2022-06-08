package net.server.channel.handlers

import client.BuffStat
import client.Client
import client.SkillFactory.Companion.getSkill
import constants.skills.*
import net.AbstractPacketHandler
import net.PacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class CancelBuffHandler : AbstractPacketHandler(), PacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.player?.let { player ->
            when (val sourceId = slea.readInt()) {
                Rogue.DARK_SIGHT -> {
                    if (player.hidden && player.isGM()) {
                        val buffStat = listOf(BuffStat.DARKSIGHT)
                        player.announce(CharacterPacket.cancelBuff(buffStat))
                        player.toggleHidden()
                        return
                    }
                    val buffStat = listOf(BuffStat.DARKSIGHT)
                    player.announce(CharacterPacket.cancelBuff(buffStat))
                    player.toggleHidden()
                }
                GM.HIDE -> {
                    val buffStat = listOf(BuffStat.DARKSIGHT)
                    player.announce(CharacterPacket.cancelBuff(buffStat))
                    player.toggleHidden()
                }
                FPArchMage.BIG_BANG, ILArchMage.BIG_BANG, Bishop.BIG_BANG, Bowmaster.HURRICANE, Marksman.PIERCING_ARROW, Corsair.RAPID_FIRE -> player.map.broadcastMessage(
                    player, CharacterPacket.skillCancel(c.player, sourceId), false
                )
                else -> player.cancelEffect(getSkill(sourceId)?.getEffect(1), false, -1)
            }
        }
    }
}