package net.server.channel.handlers

import client.BuffStat
import client.Character
import client.CharacterStat
import client.Skill
import client.SkillFactory.Companion.getSkill
import client.autoban.AutobanFactory
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.WeaponType
import client.status.MonsterStatus
import client.status.MonsterStatusEffect
import constants.ItemConstants
import constants.skills.*
import mu.KLogging
import net.AbstractPacketHandler
import server.ItemInformationProvider
import server.StatEffect
import server.life.Element
import server.life.ElementalEffectiveness
import server.life.MonsterInformationProvider
import server.maps.MapItem
import server.maps.MapObjectType
import tools.CoroutineManager
import tools.PacketCreator
import tools.data.input.LittleEndianAccessor
import tools.packet.CharacterPacket
import tools.packet.GameplayPacket
import java.awt.Point
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

abstract class AbstractDealDamageHandler : AbstractPacketHandler() {
    @Synchronized protected fun applyAttack(attack: AttackInfo, player: Character, attackCount: Int) {
        var theSkill: Skill? = null
        var attackEffect: StatEffect? = null
        try {
            if (player.banned) return
            if (attack.skill != 0) {
                theSkill = getSkill(attack.skill)
                attackEffect = attack.getAttackEffect(player, theSkill)
                if (attackEffect == null) {
                    player.client.announce(PacketCreator.enableActions())
                    return
                }
                if (player.mp < attackEffect.mpCon) {
                    AutobanFactory.MPCON.addPoint(
                        player.autoban,
                        "Skill: ${attack.skill}; Player MP: ${player.mp}; MP Needed: ${attackEffect.mpCon}"
                    )
                }
                if (attack.skill != Cleric.HEAL) {
                    if (player.isAlive()) {
                        attackEffect.applyTo(player)
                    } else {
                        player.client.announce(PacketCreator.enableActions())
                    }
                }
                var mobCount = attackEffect.mobCount
                when (attack.skill) {
                    DawnWarrior.FINAL_ATTACK, Page.FINAL_ATTACK_BW, Page.FINAL_ATTACK_SWORD,
                    Fighter.FINAL_ATTACK_SWORD, Fighter.FINAL_ATTACK_AXE, Spearman.FINAL_ATTACK_SPEAR,
                    Spearman.FINAL_ATTACK_POLEARM, Hunter.FINAL_ATTACK, Crossbowman.FINAL_ATTACK -> {
                        mobCount = 15 //:(
                    }
                }
                if (attack.numAttacked > mobCount) {
                    AutobanFactory.MOB_COUNT.autoban(
                        player,
                        "Skill: ${attack.skill}; Count: ${attack.numAttacked} Max: ${attackEffect.mobCount}"
                    )
                    return
                }
            }
            if (!player.isAlive()) return
            var totDamage = 0
            val map = player.map
            if (attack.skill == ChiefBandit.MESO_EXPLOSION) {
                var delay = 0
                attack.allDamage.keys.forEach { key ->
                    val mapObject = map.mapObjects[key]
                    if (mapObject != null && mapObject.objectType == MapObjectType.ITEM) {
                        mapObject as MapItem
                        if (mapObject.meso > 9) {
                            synchronized(mapObject) {
                                if (mapObject.pickedUp) return
                                CoroutineManager.schedule({
                                    map.removeMapObject(mapObject)
                                    map.broadcastMessage(GameplayPacket.removeItemFromMap(mapObject.objectId, 4, 0, false, 0), mapObject.position)
                                    mapObject.pickedUp = true
                                }, delay.toLong())
                                delay += 100
                            }
                        } else if (mapObject.meso == 0) return
                    } else if (mapObject != null && mapObject.objectType != MapObjectType.MONSTER) {
                        return
                    }
                }
            }
            attack.allDamage.keys.forEach { key ->
                val monster = map.getMonsterByOid(key) ?: return@forEach
                var totDamageToOneMonster = 0
                val list = attack.allDamage[key]
                list?.forEach listLoop@ { dmg ->
                    val p = attack.mobPoint[key] ?: return@listLoop
                    val d = monster.position
                    var mobX = d.x - p.x
                    var mobY = d.y - p.y
                    if (mobX > 800 || mobX < -800 || mobY > 600 || mobY < -600) {
                        player.client.disconnect(shutdown = false, cashShop = false)
                        logger.warn { "${player.name} SEMI_ITEM_MONSTER distance: $d, skill code: ${attack.skill}, X: $mobX, Y: $mobY." }
                        return
                    }
                    mobX = d.x - player.position.x
                    mobY = d.y - player.position.y
                    if (attack.skill != 1311006 && attack.skill != 5001006) {
                        if (mobX > 800 || mobX < -800 || mobY > 600 || mobY < -600) {
                            player.client.disconnect(shutdown = false, cashShop = false)
                            logger.warn { "${player.name} ATTACK_FARAWAY_MONSTER distance: $d, skill code: ${attack.skill}, X: $mobX, Y: $mobY." }
                            return
                        }
                    }
                    if (attack.skill == 2301002 && !monster.stats.undead) {
                        AutobanFactory.HEAL_ATTACKING_UNDEAD.autoban(player, "$dmg damage")
                        return
                    }
                    totDamageToOneMonster += dmg
                }
                totDamage += totDamageToOneMonster
                player.checkMonsterAggro(monster)
                when (attack.skill) {
                    Rogue.DOUBLE_STAB, Bandit.SAVAGE_BLOW, ChiefBandit.ASSAULTER, ChiefBandit.BAND_OF_THIEVES, Shadower.ASSASSINATE, Shadower.TAUNT, Shadower.BOOMERANG_STEP -> {
                        if (player.getBuffedValue(BuffStat.PICKPOCKET) != null && attack.skill == 0) {
                            val pickPocket = getSkill(ChiefBandit.PICKPOCKET)
                            var delay = 0
                            val maxMeso = player.getBuffedValue(BuffStat.PICKPOCKET) ?: 0
                            list?.forEach listLoop@ {
                                if (pickPocket?.getEffect(player.getSkillLevel(pickPocket).toInt())?.makeChanceResult() == true) {
                                    CoroutineManager.schedule({
                                        player.map.spawnMesoDrop(
                                            (it / 20000.0 * maxMeso.toDouble()).coerceAtLeast(1.0)
                                                .toInt().coerceAtMost(maxMeso), Point(
                                                monster.position.x + Random.nextInt(100) - 50,
                                                monster.position.y
                                            ), monster, player, true, 0.toByte())
                                    }, delay.toLong())
                                    delay += 100
                                }
                            }
                        }
                    }
                    Marksman.SNIPE -> totDamageToOneMonster = 195000 + Random.nextInt(5000)
                    Marauder.ENERGY_DRAIN, Assassin.DRAIN -> {
                        val skill = getSkill(attack.skill) ?: return@forEach
                        player.addHp(min(monster.getMaxHp(), min(
                                    (totDamage.toDouble() * skill
                                        .getEffect(player.getSkillLevel(skill).toInt()).x.toDouble() / 100.0).toInt(), player.maxHp / 2)))
                    }
                    Bandit.STEAL -> {
                        val steal = getSkill(Bandit.STEAL) ?: return@forEach
                        if (Math.random() < 0.3 && steal.getEffect(player.getSkillLevel(steal).toInt()).makeChanceResult()) {
                            val toSteals = MonsterInformationProvider.retrieveDrop(monster.id).shuffled()
                            val toSteal = toSteals[rand(0, (toSteals.size - 1))].itemId
                            val item = if (ItemConstants.getInventoryType(toSteal) == InventoryType.EQUIP) {
                                ItemInformationProvider.randomizeStats(ItemInformationProvider.getEquipById(toSteal) as Equip)
                            } else Item(toSteal, 0, 1, -1)
                            player.map.spawnItemDrop(monster, player, item, monster.position,
                                ffaDrop = false,
                                playerDrop = false
                            )
                            monster.addStolen(toSteal)
                        }
                    }
                    FPArchMage.FIRE_DEMON -> {
                        val skill = getSkill(FPArchMage.FIRE_DEMON) ?: return@forEach
                        monster.setTempEffectiveness(
                            Element.ICE, ElementalEffectiveness.WEAK, (skill.getEffect(player.getSkillLevel(skill).toInt()).duration * 1000).toLong())
                    }
                    ILArchMage.ICE_DEMON -> {
                        val skill = getSkill(ILArchMage.ICE_DEMON) ?: return@forEach
                        monster.setTempEffectiveness(
                            Element.FIRE, ElementalEffectiveness.WEAK, (skill.getEffect(player.getSkillLevel(skill).toInt()).duration * 1000).toLong())
                    }
                    Outlaw.HOMING_BEACON, Corsair.BULLSEYE -> {
                        player.markedMonster = monster.objectId
                        player.announce(CharacterPacket.giveBuff(1, attack.skill, listOf(Pair(BuffStat.HOMING_BEACON, monster.objectId))))
                    }
                }
                if (player.getBuffedValue(BuffStat.HAMSTRING) != null) {
                    val hamString = getSkill(Bowmaster.HAMSTRING) ?: return@forEach
                    if (hamString.getEffect(player.getSkillLevel(hamString).toInt()).makeChanceResult()) {
                        val monsterStatusEffect = MonsterStatusEffect(
                            mutableMapOf(Pair(MonsterStatus.SPEED,
                                hamString.getEffect(player.getSkillLevel(hamString).toInt()).x)), hamString, null, false
                        )
                        monster.applyStatus(player, monsterStatusEffect, false,
                            (hamString.getEffect(player.getSkillLevel(hamString).toInt()).y * 1000).toLong(), false)
                    }
                }
                if (player.getBuffedValue(BuffStat.BLIND) != null) {
                    val blind = getSkill(Marksman.BLIND) ?: return@forEach
                    if (blind.getEffect(player.getSkillLevel(blind).toInt()).makeChanceResult()) {
                        val monsterStatusEffect = MonsterStatusEffect(
                            mutableMapOf(Pair(
                                MonsterStatus.ACC, blind.getEffect(player.getSkillLevel(blind).toInt()).x)), blind, null, false)
                        monster.applyStatus(player, monsterStatusEffect, false, (blind.getEffect(player.getSkillLevel(blind).toInt()).y * 1000).toLong(), false)
                    }
                }
                val id = player.job.id
                if (id == 121 || id == 122) {
                    for (charge in 1211005..1211006) {
                        val chargeSkill = getSkill(charge) ?: continue
                        if (player.isBuffFrom(BuffStat.WK_CHARGE, chargeSkill)) {
                            val iceEffectiveness = monster.getEffectiveness(Element.ICE)
                            if (totDamageToOneMonster > 0 && iceEffectiveness === ElementalEffectiveness.NORMAL || iceEffectiveness === ElementalEffectiveness.WEAK) {
                                monster.applyStatus(player,
                                    MonsterStatusEffect(mutableMapOf(Pair(MonsterStatus.FREEZE, 1)), chargeSkill, null, false),
                                    false,
                                    (chargeSkill.getEffect(player.getSkillLevel(chargeSkill).toInt()).y * 2000).toLong(),
                                    false
                                )
                            }
                            break
                        }
                    }
                } else if (player.getBuffedValue(BuffStat.BODY_PRESSURE) != null || player.getBuffedValue(BuffStat.COMBO_DRAIN) != null) {
                    if (player.getBuffedValue(BuffStat.BODY_PRESSURE) != null) {
                        val skill = getSkill(21101003)
                        val eff = skill?.let { player.getSkillLevel(it).toInt() }?.let { skill.getEffect(it) }
                        if (eff?.makeChanceResult() == true) {
                            monster.applyStatus(player, MonsterStatusEffect(mutableMapOf(Pair(MonsterStatus.NEUTRALISE, 1)), skill, null, false), false,
                                (eff.x * 1000).toLong(), false)
                        }
                    }
                    if (player.getBuffedValue(BuffStat.COMBO_DRAIN) != null) {
                        val skill = getSkill(21100005)
                        player.setHpSilent(player.hp + ((totDamage * (skill?.getEffect(player.getSkillLevel(skill).toInt())?.x ?: 1) / 100)))
                        player.updateSingleStat(CharacterStat.HP, player.hp, false)
                    }
                } else if (id == 412 || id == 422 || id == 1411) {
                    val type =
                        getSkill(if (player.job.id == 412) 4120005 else if (player.job.id == 1411) 14110004 else 4220005) ?: return@forEach
                    if (player.getSkillLevel(type) > 0) {
                        val venomEffect = type.getEffect(player.getSkillLevel(type).toInt())
                        for (i in 0 until attackCount) {
                            if (venomEffect.makeChanceResult()) {
                                if (monster.venomMultiplier < 3) {
                                    monster.venomMultiplier = monster.venomMultiplier + 1
                                    val monsterStatusEffect = MonsterStatusEffect(
                                        mutableMapOf(Pair(MonsterStatus.POISON, 1)), type, null, false)
                                    monster.applyStatus(player, monsterStatusEffect, false, venomEffect.duration.toLong(), true)
                                }
                            }
                        }
                    }
                }
                if (totDamageToOneMonster > 0 && attackEffect != null && attackEffect.monsterStatus?.isNotEmpty() == true) {
                    if (attackEffect.makeChanceResult()) {
                        attackEffect.monsterStatus?.toMutableMap()?.let {
                            monster.applyStatus(player, MonsterStatusEffect(it, theSkill, null, false), attackEffect.isPoison(),
                                attackEffect.duration.toLong(), false)
                        }
                    }
                }
                if (attack.isHH && !monster.isBoss()) {
                    map.damageMonster(player, monster, monster.hp - 1)
                } else if (attack.isHH) {
                    val hh = getSkill(Paladin.HEAVENS_HAMMER) ?: return@forEach
                    val hhDamage = player.calculateMaxBaseDamage(player.watk) * hh.getEffect(player.getSkillLevel(hh).toInt()).damage / 100
                    map.damageMonster(player, monster, floor(Math.random() * (hhDamage / 5) + hhDamage * .8).toInt())
                } else {
                    map.damageMonster(player, monster, totDamageToOneMonster)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error caused apply attack." }
        }
    }

    protected fun parseDamage(lea: LittleEndianAccessor, chr: Character, ranged: Boolean): AttackInfo {
        val ret = AttackInfo()
        ret.numAttackedAndDamage = lea.readByte().toInt()
        ret.numAttacked = ret.numAttackedAndDamage ushr 4 and 0xF
        ret.numDamage = ret.numAttackedAndDamage and 0xF
        ret.skill = lea.readInt()
        if (ret.skill > 0) {
            ret.skillLevel = chr.getSkillLevel(ret.skill).toInt()
        }
        when (ret.skill) {
            FPArchMage.BIG_BANG, ILArchMage.BIG_BANG, Bishop.BIG_BANG, Gunslinger.GRENADE, Brawler.CORKSCREW_BLOW -> {
                ret.charge = lea.readInt()
            }
        }
        if (ret.skill == Paladin.HEAVENS_HAMMER) ret.isHH = true
        ret.display = lea.readByte().toInt()
        ret.stance = lea.readByte().toInt()
        if (ret.skill == ChiefBandit.MESO_EXPLOSION) {
            if (ret.numAttackedAndDamage == 0) {
                lea.skip(9)
                val bullets = lea.readByte()
                for (j in 0 until bullets) {
                    val mesoId = lea.readInt()
                    lea.skip(1)
                    ret.allDamage[mesoId] = null
                }
                return ret
            } else {
                 lea.skip(5)
            }
            for (i in 0 until ret.numAttacked + 1) {
                if (i < ret.numAttacked) {
                    val oid = lea.readInt()
                    lea.skip(12)
                    val bullets = lea.readByte()
                    val allDamageNumbers = mutableListOf<Int>()
                    for (j in 0 until bullets) {
                        val damage = lea.readInt()
                        allDamageNumbers.add(damage)
                    }
                    ret.allDamage[oid] = allDamageNumbers
                } else {
                    lea.skip(4)
                    val bullets = lea.readByte()
                    for (j in 0 until bullets) {
                        val mesoId = lea.readInt()
                        lea.skip(1)
                        ret.allDamage[mesoId] = null
                    }
                }
            }
            return ret
        }
        if (ranged) {
            ret.speed = lea.readByte().toInt()
            lea.readByte()
            ret.rangeDirection = lea.readByte().toInt()
            lea.skip(7)
            when (ret.skill) {
                Bowmaster.HURRICANE, Marksman.PIERCING_ARROW, Corsair.RAPID_FIRE -> lea.skip(4)
            }
        } else {
            ret.speed = lea.readByte().toInt()
            val tick = lea.readInt()
            val subTick = tick - chr.attackTick
            if (subTick < 300) {
                AutobanFactory.FAST_ATTACK.autoban(chr, "비정상적으로 빠른 공격.")
            }
            chr.attackTick = tick
        }
        val weaponId = chr.getInventory(InventoryType.EQUIPPED)?.getItem(-11)?.itemId ?: -1
        val weaponType = ItemInformationProvider.getWeaponType(weaponId)
        ret.mastery = getMastery(chr, weaponType).toInt()
        for (i in 0 until ret.numAttacked) {
            val oid = lea.readInt()
            lea.skip(4)
            val mobX = lea.readShort().toInt()
            val mobY = lea.readShort().toInt()
            lea.skip(6)
            val allDamageNumbers = mutableListOf<Int>()
            for (j in 0 until ret.numDamage) {
                var damage = lea.readInt()
                if (ret.skill == Marksman.SNIPE) damage += 0x80000000.toInt() //Critical
                allDamageNumbers.add(damage)
            }
            ret.allDamage[oid] = allDamageNumbers
            ret.mobPoint[oid] = Point(mobX, mobY)
        }
        return ret
    }

    companion object : KLogging() {
        class AttackInfo {
            var numAttacked = 0
            var numDamage = 0
            var numAttackedAndDamage = 0
            var skill = 0
            var skillLevel = 0
            var stance = 0
            //var direction = 0
            var rangeDirection = 0
            var charge = 0
            var display = 0
            var mastery = 0
            var allDamage: MutableMap<Int, List<Int>?> = mutableMapOf()
            var mobPoint: MutableMap<Int, Point> = mutableMapOf()
            var isHH = false
            var speed = 4

            fun getAttackEffect(chr: Character, theSkill: Skill?): StatEffect? {
                val mySkill = theSkill ?: getSkill(skill)
                var skillLevel = mySkill?.let { chr.getSkillLevel(it).toInt() } ?: return null
                if (skillLevel == 0) return null
                if (display > 80) { //Hmm
                    if (theSkill?.action != true) {
                        AutobanFactory.FAST_ATTACK.autoban(chr, "WZ Edit; adding action to a skill: $display")
                        return null
                    }
                }
                return mySkill.getEffect(skillLevel)
            }
        }

        private fun getMastery(chr: Character, weaponType: WeaponType): Double {
            return when (weaponType) {
                WeaponType.SWORD1H, WeaponType.SWORD2H -> if (chr.getSkillLevel(1100000) > 0) {
                        ceil(chr.getSkillLevel(1100000) / 2.0)
                    } else if (chr.getSkillLevel(1200000) > 0) {
                        ceil(chr.getSkillLevel(1200000) / 2.0)
                    } else 0.0
                WeaponType.AXE1H, WeaponType.AXE2H -> if (chr.getSkillLevel(1100001) > 0) {
                        ceil(chr.getSkillLevel(1100001) / 2.0)
                    } else 0.0
                WeaponType.BLUNT1H, WeaponType.BLUNT2H -> if (chr.getSkillLevel(1200001) > 0) {
                    ceil(chr.getSkillLevel(1200001) / 2.0)
                } else 0.0
                WeaponType.DAGGER -> if (chr.getSkillLevel(4200000) > 0) {
                    ceil(chr.getSkillLevel(4200000) / 2.0)
                } else 0.0
                WeaponType.SPEAR -> if (chr.getSkillLevel(1300000) > 0) {
                    ceil(chr.getSkillLevel(1300000) / 2.0)
                } else 0.0
                WeaponType.POLE_ARM -> if (chr.getSkillLevel(1300001) > 0) {
                    ceil(chr.getSkillLevel(1300001) / 2.0)
                } else 0.0
                else -> return 0.0
            }
        }

        fun rand(l: Int, u: Int) = ((Math.random() * (u - l + 1)) + l).toInt()
    }
}