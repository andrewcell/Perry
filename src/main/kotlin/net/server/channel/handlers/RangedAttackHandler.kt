package net.server.channel.handlers

import client.BuffStat
import client.Character
import client.Client
import client.SkillFactory
import client.inventory.InventoryType
import client.inventory.WeaponType
import constants.ItemConstants
import constants.skills.Buccaneer
import constants.skills.NightLord
import constants.skills.Shadower
import server.InventoryManipulator
import server.ItemInformationProvider
import server.StatEffect
import tools.CoroutineManager
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import kotlin.random.Random

class RangedAttackHandler : AbstractDealDamageHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val player = c.player ?: return
        val attack = parseDamage(slea, player, true)
        with (attack) {
            when (this.skill) {
                Buccaneer.ENERGY_ORB, Shadower.TAUNT, NightLord.TAUNT -> {
                    player.map.broadcastMessage(player, CharacterPacket.rangedAttack(player, this.skill, this.skill, this.stance, this.numAttackedAndDamage, 0, this.allDamage, this.speed, this.mastery), false)
                }
                else -> {
                    val weapon = player.getInventory(InventoryType.EQUIPPED)?.getItem(-11) ?: return
                    val type = ItemInformationProvider.getWeaponType(weapon.itemId)
                    if (type == WeaponType.NOT_A_WEAPON) return
                    var projectile = 0
                    var bulletCount = 1
                    var effect: StatEffect? = null
                    if (this.skill != 0) {
                        effect = attack.getAttackEffect(player, null)
                        val coolTime = SkillFactory.getSkill(this.skill)?.coolTime ?: 0
                        bulletCount = effect?.bulletCount?.toInt() ?: 0
                        if (coolTime > 0) c.announce(CharacterPacket.skillCoolDown(this.skill, coolTime))
                    }
                    val hasShadowPartner = player.getBuffedValue(BuffStat.SHADOWPARTNER) != null
                    if (hasShadowPartner) bulletCount *= 2
                    val inv = player.getInventory(InventoryType.USE) ?: return
                    for (i in 0..inv.slotLimit) {
                        val item = inv.getItem(i.toByte()) ?: continue
                        val id = item.itemId
                        val bow = ItemConstants.isArrowForBow(id)
                        val cBow = ItemConstants.isArrowForCrossBow(id)
                        if (item.quantity > if (bulletCount == 1) 0 else bulletCount) {
                            if (type == WeaponType.CLAW && ItemConstants.isThrowingStar(id) && weapon.itemId != 1472063) {
                                if (!(((id == 2070007 || id == 2070018) && player.level < 70) || (id == 2070016 && player.level < 50))) {
                                    projectile = id
                                    break
                                }
                            } else if ((type == WeaponType.GUN && ItemConstants.isBullet(id))) {
                                /*if (id == 2331000 && id == 2332000) {
                                    if (player.level > 69) {
                                        projectile = id
                                        break
                                    }
                                }*/
                                if (player.level > (id % 10) * 20 * 9) {
                                    projectile = id
                                    break
                                }
                            } else if ((type == WeaponType.BOW && bow) || (type == WeaponType.CROSSBOW && cBow) || (weapon.itemId == 1472063 && (bow || cBow))) {
                                projectile = id
                                break
                            }
                        }
                    }
                    val soulArrow = player.getBuffedValue(BuffStat.SOULARROW) != null
                    val shadowClaw = player.getBuffedValue(BuffStat.SHADOW_CLAW) != null
                    if (!soulArrow && !shadowClaw && this.skill != 11101004 && this.skill != 15111007 && this.skill != 14101006) {
                        var bulletConsume = bulletCount
                        if (effect != null && effect.bulletConsume.toInt() != 0) {
                            bulletConsume = (effect.bulletConsume * (if (hasShadowPartner) 2 else 1))
                        }
                        InventoryManipulator.removeById(c, InventoryType.USE, projectile, bulletConsume,
                            fromDrop = false,
                            consume = true
                        )
                    }
                    if (projectile != 0 || soulArrow || this.skill == 11101004 || this.skill == 15111007 || this.skill == 14101006) {
                        var visibleProjectile = projectile
                        if (ItemConstants.isThrowingStar(projectile)) {
                            val cash = player.getInventory(InventoryType.CASH)
                            for (i in 0..95) { // impose order...
                                val item = cash?.getItem(i.toByte()) ?: continue
                                if (item.itemId / 1000 == 5021) {
                                    visibleProjectile = item.itemId
                                    break
                                }
                            }
                        } else {
                            if (soulArrow || this.skill == 3111004 || this.skill == 3211004 || this.skill == 11101004 || this.skill == 15111007 || this.skill == 14101006) {
                                visibleProjectile = 0
                            }
                            val packet = when (this.skill) {
                                3121004, 3221001, 5221004, 13111002 -> CharacterPacket.rangedAttack(player, attack.skill, attack.skillLevel, attack.rangeDirection, attack.numAttackedAndDamage, visibleProjectile, attack.allDamage, attack.speed, 0)
                                else -> CharacterPacket.rangedAttack(player, attack.skill, attack.skillLevel, attack.stance, attack.numAttackedAndDamage, visibleProjectile, attack.allDamage, attack.speed, 0)
                            }
                            player.map.broadcastMessage(player, packet, repeatToSource = false, ranged = true)
                            if (effect != null) {
                                var money = effect.moneyCon
                                if (money != 0) {
                                    val moneyMod = money / 2
                                    money += Random.nextInt(moneyMod)
                                    if (money > player.meso.get()) {
                                        money = player.meso.get()
                                    }
                                    player.gainMeso(-money, false)
                                }
                            }
                            if (this.skill != 0) {
                                val coolTime = SkillFactory.getSkill(this.skill)?.coolTime ?: 0
                                if (coolTime > 0) {
                                    if (player.skillIsCooling(this.skill)) return
                                    else {
                                        c.announce(CharacterPacket.skillCoolDown(this.skill, coolTime))
                                        player.addCoolDown(this.skill, System.currentTimeMillis(),
                                            (coolTime * 1000).toLong(), CoroutineManager.schedule(Character.Companion.CancelCoolDownAction(player, this.skill),
                                                (coolTime * 1000).toLong()
                                            ))
                                    }
                                }
                            }
                            if (player.getBuffedValue(BuffStat.DARKSIGHT) != null && attack.numAttacked > 0 && player.getBuffSource(
                                    BuffStat.DARKSIGHT) != 9101004
                            ) {
                                player.cancelEffectFromBuffStat(BuffStat.DARKSIGHT)
                                player.cancelBuffStats(BuffStat.DARKSIGHT)
                            }
                            applyAttack(attack, player, bulletCount)
                        }
                    }
                }
            }
        }
    }
}