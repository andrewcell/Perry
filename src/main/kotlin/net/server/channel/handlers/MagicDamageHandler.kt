package net.server.channel.handlers

import client.Character.Companion.CancelCoolDownAction
import client.Client
import client.SkillFactory.Companion.getSkill
import tools.CoroutineManager
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class MagicDamageHandler : AbstractDealDamageHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val player = c.player ?: return
        val attack = parseDamage(slea, player, false)
        var packet = CharacterPacket.magicAttack(
            player,
            attack.skill,
            attack.skillLevel,
            attack.stance,
            attack.numAttackedAndDamage,
            attack.allDamage.toMap(),
            -1,
            attack.speed,
            attack.mastery
        )
        if (attack.skill == 2121001 || attack.skill == 2221001 || attack.skill == 2321001) {
            packet = CharacterPacket.magicAttack(
                player,
                attack.skill,
                attack.skillLevel,
                attack.stance,
                attack.numAttackedAndDamage,
                attack.allDamage.toMap(),
                attack.charge,
                attack.speed,
                attack.mastery
            )
        }
        player.map.broadcastMessage(player, packet, repeatToSource = false, ranged = true)
        val effect = attack.getAttackEffect(player, null)
        val coolTime = getSkill(attack.skill)?.coolTime ?: 0
        if (coolTime > 0) {
            if (player.skillIsCooling(attack.skill)) {
                return
            } else {
                c.announce(CharacterPacket.skillCoolDown(attack.skill, coolTime))
                player.addCoolDown(
                    attack.skill, System.currentTimeMillis(), (coolTime * 1000).toLong(), CoroutineManager.schedule(
                        CancelCoolDownAction(
                            player, attack.skill
                        ), (coolTime * 1000).toLong()
                    )
                )
            }
        }
        applyAttack(attack, player, effect?.attackCount ?: 0)
        getSkill((player.job.id - player.job.id % 10) * 10000)?.let {// MP Eater, works with right job
            val eaterLevel = player.getSkillLevel(it).toInt()
            if (eaterLevel > 0) {
                for (singleDamage in attack.allDamage.keys) {
                    player.map.mapObjects[singleDamage]?.let { it1 ->
                        it.getEffect(eaterLevel).applyPassive(player,
                            it1, 0)
                    }
                }
            }
        }
    }
}