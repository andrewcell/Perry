package net.server.channel.handlers

import client.*
import constants.skills.Crusader
import constants.skills.DragonKnight
import constants.skills.Hero
import constants.skills.Rogue
import tools.CoroutineManager
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class CloseRangeDamageHandler : AbstractDealDamageHandler() {
    private fun isFinisher(skillId: Int): Boolean {
        return skillId in 1111003..1111006 || skillId == 11111002 || skillId == 11111003
    }

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val player = c.player ?: return
        val attack = parseDamage(slea, player, false)
        player.map.broadcastMessage(
            player,
            CharacterPacket.closeRangeAttack(
                player,
                attack.skill,
                attack.skillLevel,
                attack.stance,
                attack.numAttackedAndDamage,
                attack.allDamage.toMap(),
                attack.speed,
                attack.mastery
            ),
            repeatToSource = false, ranged = true)
        var numFinisherOrbs = 0
        val comboBuff = player.getBuffedValue(BuffStat.COMBO)
        if (isFinisher(attack.skill)) {
            comboBuff?.let { numFinisherOrbs = it - 1 }
            player.handleOrbconsume()
        } else if (attack.numAttacked > 0) {
            if (attack.skill != 1111008 && comboBuff != null) {
                val orbCount = player.getBuffSource(BuffStat.COMBO)
                val oid = Crusader.COMBO
                val advComboId = Hero.ADVANCED_COMBO
                val combo = SkillFactory.getSkill(oid)
                val advCombo = SkillFactory.getSkill(advComboId)
                val advComboSkillLevel = advCombo?.let { player.getSkillLevel(it) } ?: 0
                val cEffect = if (advComboSkillLevel > 0) {
                    advCombo?.getEffect(advComboSkillLevel.toInt())
                } else combo?.getEffect(player.getSkillLevel(combo).toInt())
                if (cEffect == null) return
                if (orbCount < cEffect.x + 1) {
                    var newOrbCount = orbCount + 1
                    if (advComboSkillLevel > 0 && cEffect.makeChanceResult()) {
                        if (newOrbCount <= cEffect.x) newOrbCount++
                    }
                    var duration = combo?.getEffect(player.getSkillLevel(oid).toInt())?.duration ?: -1
                    val stat = listOf(Pair(BuffStat.COMBO, newOrbCount))
                    player.setBuffedValue(BuffStat.COMBO, newOrbCount)
                    duration -= (System.currentTimeMillis() - (player.getBuffedStartTime(BuffStat.COMBO)?.toInt() ?: 0)).toInt()
                    c.announce(CharacterPacket.giveBuff(oid, duration, stat))
                    player.map.broadcastMessage(player, CharacterPacket.giveForeignBuff(player.id, stat), false)
                }
            }
        }
        if (attack.numAttacked > 0 && attack.skill == DragonKnight.SACRIFICE) {
            var totDamageToOneMonster = 0 // sacrifice attacks only 1 mob with 1 attack
            attack.allDamage.values.forEach {
                totDamageToOneMonster = it?.get(0) ?: 0
            }
            val remainingHp = player.hp - totDamageToOneMonster * attack.getAttackEffect(player, null)?.x!! / 100
            if (remainingHp > 1)
                player.setHpNormal(remainingHp)
            else player.setHpNormal(1)
            player.updateSingleStat(CharacterStat.HP, player.hp, false)
            player.checkBerserk()
        }
        if (attack.numAttacked > 0 && attack.skill == 1211002) {
            var advChargeProb = false
            val advChargeLevel = SkillFactory.getSkill(1220010)?.let { player.getSkillLevel(it) } ?: 0
            if (advChargeLevel > 0) {
                advChargeProb = SkillFactory.getSkill(1220010)?.getEffect(advChargeLevel.toInt())?.makeChanceResult() ?: false
            }
            if (!advChargeProb) player.cancelEffectFromBuffStat(BuffStat.WK_CHARGE)
        }
        val attackCount = if (attack.skill != 0) {
            attack.getAttackEffect(player, null)?.attackCount
        } else 0
        if (numFinisherOrbs == 0 && isFinisher(attack.skill)) return
        if (attack.skill > 0) {
            val coolTime = SkillFactory.getSkill(attack.skill)?.coolTime ?: 0
            if (coolTime > 0) {
                if (player.skillIsCooling(attack.skill)) return
                else {
                    c.announce(CharacterPacket.skillCoolDown(attack.skill, coolTime))
                    player.addCoolDown(attack.skill, System.currentTimeMillis(),
                        (coolTime * 1000).toLong(), CoroutineManager.schedule(Character.Companion.CancelCoolDownAction(player, attack.skill),
                        (coolTime * 1000).toLong()
                    ))
                }
            }
        }
        val darkSight = SkillFactory.getSkill(Rogue.DARK_SIGHT)?.let { player.getSkillLevel(it) } ?: 0
        if (darkSight > 0 && player.getBuffedValue(BuffStat.DARKSIGHT) != null) {
            player.cancelEffectFromBuffStat(BuffStat.DARKSIGHT)
            player.cancelBuffStats(BuffStat.DARKSIGHT)
        }
        applyAttack(attack, player, attackCount ?: 0)
    }
}