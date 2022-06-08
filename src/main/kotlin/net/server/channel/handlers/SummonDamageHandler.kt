package net.server.channel.handlers

import client.Client
import client.SkillFactory
import client.status.MonsterStatusEffect
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class SummonDamageHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val summonSkillId = slea.readInt()
        val player = c.player ?: return
        if (!player.isAlive()) return
        val summon = player.summons[summonSkillId] ?: return
        val summonSkill = SkillFactory.getSkill(summon.skill)
        val summonEffect = summonSkill?.getEffect(summon.skillLevel.toInt()) ?: return
        val direction = slea.readByte()
        val monsterOid = slea.readInt()
        slea.skip(14)
        val damage = slea.readInt()
        player.map.broadcastMessage(player, CharacterPacket.summonAttack(player.id, summonSkillId, direction, monsterOid, damage), summon.position)
        val target = player.map.getMonsterByOid(monsterOid) ?: return
        val monsterStatus = summonEffect.monsterStatus
        if (damage > 0 && monsterStatus != null && monsterStatus.isNotEmpty()) {
            if (summonEffect.makeChanceResult()) {
                target.applyStatus(player, MonsterStatusEffect(monsterStatus.toMutableMap(), summonSkill, null, false), summonEffect.isPoison(), 4000, false)
            }
        }
        player.map.damageMonster(player, target, damage)
    }

    data class SummonAttackEntry(val monsterOid: Int, val damage: Int)
}