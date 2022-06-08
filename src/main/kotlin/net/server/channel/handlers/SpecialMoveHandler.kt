package net.server.channel.handlers

import client.Character
import client.CharacterStat
import client.Client
import client.SkillFactory
import constants.skills.*
import net.AbstractPacketHandler
import net.server.Server
import tools.CoroutineManager
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import java.awt.Point

class SpecialMoveHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        val skillId = slea.readInt()
        val skillLevelReceived = slea.readByte()
        val skill = SkillFactory.getSkill(skillId)
        var skillLevel = skill?.let { chr.getSkillLevel(it) } ?: 0
        if (skillId % 10000000 == 1010 || skillId % 10000000 == 1011) {
            skillLevel = 1
            /*chr.dojoEnergy = 0
            c.announce(PacketCreator.getEnergy("energy", 0))*/
        }
        if (skillLevel.toInt() == 0 || skillLevel != skillLevelReceived) return
        val coolTime = SkillFactory.getSkill(skillId)?.coolTime ?: 0
        if (coolTime > 0) {
            if (chr.skillIsCooling(skillId)) return
            else if (skillId != Corsair.BATTLE_SHIP) {
                c.announce(CharacterPacket.skillCoolDown(skillId, coolTime))
                val timer = CoroutineManager.schedule(Character.Companion.CancelCoolDownAction(chr, skillId), coolTime * 1000.toLong())
                chr.addCoolDown(skillId, System.currentTimeMillis(), (coolTime * 1000).toLong(), timer)
            }
        }
        when (skillId) {
            Hero.MONSTER_MAGNET, Paladin.MONSTER_MAGNET, DarkKnight.MONSTER_MAGNET -> {
                val num = slea.readInt()
                var mobId: Int
                var success: Int
                for (i in 0 until num) {
                    mobId = slea.readInt()
                    success = slea.readByte().toInt()
                    chr.map.broadcastMessage(chr, CharacterPacket.showMagnet(mobId, success.toByte()), false)
                    val monster = chr.map.getMonsterByOid(mobId)
                    monster?.switchController(chr, monster.controllerHasAggro)
                }
                val direction = slea.readByte()
                chr.map.broadcastMessage(chr, PacketCreator.showBuffEffect(chr.id, skillId,
                    chr.getSkillLevel(skillId).toInt(), direction), false)
                c.announce(PacketCreator.enableActions())
                return
            }
            Buccaneer.TIME_LEAP -> { // Time leap
                val p = chr.party
                p?.members?.forEach { m ->
                    Server.getChannelsFromWorld(c.world).forEach { c ->
                        m.id?.let { c.players.getCharacterById(it)?.removeAllCoolDownsExcept(5121010) }
                    }
                }
                chr.removeAllCoolDownsExcept(Buccaneer.TIME_LEAP)
            }
            Brawler.MP_RECOVERY -> {
                val s = SkillFactory.getSkill(skillId) ?: return
                val ef = s.getEffect(chr.getSkillLevel(s).toInt())
                val lose = chr.maxHp / ef.x
                chr.setHpNormal(chr.hp - lose)
                chr.updateSingleStat(CharacterStat.HP, chr.hp)
                val gain = lose * (ef.y / 100)
                chr.setMpNormal(chr.mp + gain)
                chr.updateSingleStat(CharacterStat.MP, chr.mp)
            }
            else -> {
                if (skillId % 10000000 == 1004) slea.readShort()
            }
        }
        var pos = if (slea.available().toInt() == 5) Point(slea.readShort().toInt(), slea.readShort().toInt()) else null
        pos = when (skillId) {
            Priest.SUMMON_DRAGON, Ranger.SILVER_HAWK, Ranger.PUPPET, Sniper.GOLDEN_EAGLE, Sniper.PUPPET -> chr.position
            else -> pos
        }
        if (chr.isAlive()) {
            if (skill != null) {
                if (skill.id != Priest.MYSTIC_DOOR || chr.canDoor) {
                    skill.getEffect(skillLevel.toInt()).applyTo(chr, pos)
                } else {
                    chr.message("Please wait 5 seconds before casting Mystic Door again")
                    c.announce(PacketCreator.enableActions())
                }
            }
        } else c.announce(PacketCreator.enableActions())
    }
}