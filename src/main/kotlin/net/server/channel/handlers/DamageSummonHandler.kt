package net.server.channel.handlers

import client.BuffStat
import client.Client
import client.SkillFactory.Companion.getSkill
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket

class DamageSummonHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val skillId = slea.readInt() //Bugged? might not be skill id.
        val unkByte = slea.readByte().toInt()
        val damage = slea.readInt()
        val monsterIdFrom = slea.readInt()
        if (getSkill(skillId) != null) {
            val player = c.player ?: return
            val summon = player.summons[skillId]
            if (summon != null) {
                summon.addHp(-damage)
                if (summon.hp <= 0) {
                    player.cancelEffectFromBuffStat(BuffStat.PUPPET)
                }
            }
            player.map.broadcastMessage(
                player,
                GameplayPacket.damageSummon(player.id, skillId, damage, unkByte, monsterIdFrom),
                summon?.position
            )
        }
    }
}