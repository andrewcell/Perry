package net.server.channel.handlers

import client.Client
import constants.skills.*
import mu.KLoggable
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class SkillEffectHandler : AbstractPacketHandler(), KLoggable {
    override val logger = logger()

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val skillId = slea.readInt()
        val level = slea.readByte().toInt()
        val flags = slea.readByte()
        val speed = slea.readByte().toInt()
        when (skillId) {
            FPMage.EXPLOSION, FPArchMage.BIG_BANG, ILArchMage.BIG_BANG, Bishop.BIG_BANG, Bowmaster.HURRICANE, Marksman.PIERCING_ARROW, ChiefBandit.CHAKRA, Brawler.CORKSCREW_BLOW, Gunslinger.GRENADE, Corsair.RAPID_FIRE, Paladin.MONSTER_MAGNET, DarkKnight.MONSTER_MAGNET, Hero.MONSTER_MAGNET -> {
                c.player?.let { it.map.broadcastMessage(it, CharacterPacket.skillEffect(it, skillId, level, flags, speed), false) }
                return
            }
            else -> {
                logger.warn { "${c.player.toString()} entered SkillEffectHandler without being handled using $skillId." }
                return
            }
        }
    }
}