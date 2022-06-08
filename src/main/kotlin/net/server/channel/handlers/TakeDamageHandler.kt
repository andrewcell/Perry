package net.server.channel.handlers

import client.BuffStat
import client.Client
import client.Skill
import client.SkillFactory
import constants.skills.Corsair
import net.AbstractPacketHandler
import server.life.MobAttackInfoFactory
import server.life.MobSkillFactory
import server.life.Monster
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import tools.packet.GameplayPacket
import kotlin.math.min
import kotlin.math.round

/*
1F 00
EA 68 D3 00
FF
00
00 00 00 00

05 87 01 00
30 A1 07 00

00
00 00 00 */
class TakeDamageHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val player = c.player ?: return
        val damageFrom = slea.readByte().toInt()
        var damage = slea.readInt()
        var oid = 0
        var monsterIdFrom = 0
        val pgmr = 0
        var direction = 0
        val posX = 0
        val posY = 0
        var fake = 0
        val isPgmr = false
        val isPg = true
        var mpAttack = 0
        var attacker: Monster? = null

        /*if (player.isGM()) {
            player.client.announce(PacketCreator.enableActions())
            return
        }*/

        val map = player.map
        if (damageFrom != -2 && damageFrom != -3) {
            monsterIdFrom = slea.readInt()
            direction = slea.readShort().toInt()
            if (direction > 1) {
                oid = slea.readInt()
                attacker = map.mapObjects[oid] as Monster
            }
        }
        if (damageFrom != -1 && damageFrom != -2 && attacker != null) {
            val attackInfo = MobAttackInfoFactory.getMobAttackInfo(attacker, damageFrom)
            if (attackInfo.isDeadlyAttack) mpAttack = player.mp - 1
            mpAttack += attackInfo.mpBurn
            val skill = MobSkillFactory.getMobSkill(attackInfo.diseaseSkill, attackInfo.diseaseLevel)
            if (damage > 0) skill.applyEffect(player, attacker, false)
            attacker.mp = attacker.mp - attackInfo.mpCon
            if (player.getBuffedValue(BuffStat.MANA_REFLECTION) != null && damage > 0 && !attacker.isBoss()) {
                val jobId = player.job.id
                if (jobId == 212 || jobId == 222 || jobId == 232) {
                    val id = jobId * 10000 + 2
                    val manaReflectSkill = SkillFactory.getSkill(id) ?: return
                    if (player.isBuffFrom(BuffStat.MANA_REFLECTION, manaReflectSkill) && player.getSkillLevel(manaReflectSkill) > 0 && manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill).toInt()).makeChanceResult()) {
                        var bouncedDamage = (damage * manaReflectSkill.getEffect(player.getSkillLevel(manaReflectSkill).toInt()).x / 100)
                        if (bouncedDamage > attacker.stats.hp / 5) {
                            bouncedDamage = attacker.stats.hp / 5
                        }
                        map.damageMonster(player, attacker, bouncedDamage)
                        map.broadcastMessage(player, GameplayPacket.damageMonster(oid, bouncedDamage), true)
                        player.client.announce(CharacterPacket.showOwnBuffEffect(id, 5))
                        map.broadcastMessage(player, PacketCreator.showBuffEffect(player.id, id, 5), false)
                    }
                }
            }
        }
        if (damage == -1) fake = 4020002 + (player.job.id / 10 - 40) * 100000
        if (damage == 0) {
            player.autoban.addMiss()
        } else {
            player.autoban.resetMisses()
        }
        if (damage > 0 && !player.hidden) {
            if (attacker != null && !attacker.isBoss()) {
                if (damageFrom == -1 && player.getBuffedValue(BuffStat.POWERGUARD) != null) {
                    var bouncedDamage = (damage * ((player.getBuffedValue(BuffStat.POWERGUARD)?.toDouble() ?: 1.0) / 100)).toInt()
                    bouncedDamage = min(bouncedDamage, attacker.stats.hp / 10)
                    map.damageMonster(player, attacker, bouncedDamage)
                    damage -= bouncedDamage
                    map.broadcastMessage(player, GameplayPacket.damageMonster(oid, bouncedDamage),
                        repeatToSource = false,
                        ranged = true
                    )
                }
            }
            if (damageFrom != -3) {
                var achilles = 0
                var achilles1: Skill? = null
                val jobId = player.job.id
                if (jobId < 200 && jobId % 10 == 2) {
                    achilles1 = SkillFactory.getSkill(jobId * 10000 + if (jobId == 112) 4 else 5)
                    achilles = player.getSkillLevel(achilles).toInt()
                }
                if (achilles != 0 && achilles1 != null) {
                    damage *= (achilles1.getEffect(achilles).x / 1000.0 * damage).toInt()
                }
            }
            val mesoGuard = player.getBuffedValue(BuffStat.MESOGUARD)
            if (player.getBuffedValue(BuffStat.MAGIC_GUARD) != null && mpAttack == 0) {
                var mpLoss = (damage * ((player.getBuffedValue(BuffStat.MAGIC_GUARD)?.toDouble() ?: 1.0) / 100.0)).toInt()
                var hpLoss = damage - mpLoss
                if (mpLoss > player.mp) {
                    hpLoss += mpLoss - player.mp
                    mpLoss = player.mp
                }
                player.addMpHp(-hpLoss, -mpLoss)
            } else if (mesoGuard != null) {
                damage = round((damage / 2).toDouble()).toInt()
                val mesoLoss = (damage * (mesoGuard.toDouble() / 100.0)).toInt()
                if (player.meso.get() < mesoLoss) {
                    player.gainMeso(-player.meso.get(), false)
                    player.cancelBuffStats(BuffStat.MESOGUARD)
                } else {
                    player.gainMeso(-mesoLoss, false)
                }
                player.addMpHp(-damage, -mpAttack)
            } else {
                if (player.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
                    if (player.getBuffedValue(BuffStat.MONSTER_RIDING) == Corsair.BATTLE_SHIP) {
                        player.decreaseBattleshipHp(damage)
                    }
                }
                player.addMpHp(-damage, mpAttack)
            }
        }
        if (!player.hidden) {
            map.broadcastMessage(player, GameplayPacket.damagePlayer(damageFrom, monsterIdFrom, player.id, damage, fake, direction, isPgmr, pgmr, isPg, oid, posX, posY), false)
            player.checkBerserk()
        }
        /*if (map.mapId in 925020000..925029999) {
            /layer.dojoEnergy =
                if (player.isGM) 300 else if (player.dojoEnergy < 300) player.dojoEnergy + 1 else 0 // gm's
            player.client.announce(PacketCreator.getEnergy("energy", player.dojoEnergy))
        }*/
    }
}