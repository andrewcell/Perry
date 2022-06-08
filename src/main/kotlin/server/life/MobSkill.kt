package server.life

import client.Character
import client.Disease
import client.status.MonsterStatus
import server.maps.MapObject
import server.maps.MapObjectType
import server.maps.Mist
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.ceil
import kotlin.random.Random

class MobSkill(
    val skillId: Int, val skillLevel: Int, private val spawnEffect: Int,
    val hp: Int, val mpCon: Int, val x: Int, val y: Int,
    val duration: Long, val coolTime: Long, val prop: Float,
    val limit: Int, val lt: Point, val rb: Point
) {
    private val toSummon = mutableListOf<Int>()

    fun addSummons(s: List<Int>) = toSummon + s

    private fun makeChanceResult() = prop.toDouble() == 1.0 || Math.random() < prop

    fun applyEffect(chr: Character, monster: Monster, skill: Boolean) {
        var disease: Disease? = null
        val stats = mutableMapOf<MonsterStatus, Int>()
        val reflection = mutableListOf<Int>()
        when (skillId) {
            100, 110, 150 -> stats[MonsterStatus.WEAPON_ATTACK_UP] = x
            101, 111, 151 -> stats[MonsterStatus.MAGIC_ATTACK_UP] = x
            102, 112, 152 -> stats[MonsterStatus.WEAPON_DEFENSE_UP] = x
            103, 113, 153 -> stats[MonsterStatus.MAGIC_DEFENSE_UP] = x
            114 -> {
                if (skill) {
                    val objects = getObjectsInRange(monster, MapObjectType.MONSTER)
                    val hps = x / 1000 * (950 + 1050 * Math.random())
                    objects.forEach {
                        (it as Monster).heal(hps.toInt(), y)
                    }
                } else monster.heal(x, y)
            }
            120 -> disease = Disease.SEAL
            121 -> disease = Disease.DARKNESS
            122 -> disease = Disease.WEAKEN
            123 -> disease = Disease.STUN
            124 -> disease = Disease.CURSE
            125 -> disease = Disease.POISON
            126 -> disease = Disease.SLOW
            127 -> {
                if (skill) {
                    getPlayersInRange(monster, chr).forEach {
                        it.dispel()
                    }
                } else chr.dispel()
            }
            128 -> disease = Disease.SEDUCE
            129 -> {
                if (skill) {
                    getPlayersInRange(monster, chr).forEach {
                        val banish = monster.stats.banishInfo
                        if (banish != null) {
                            it.changeMapBanish(
                                banish.map,
                                banish.portal,
                                banish.message
                            )
                        }
                    }
                } else monster.stats.banishInfo?.let { chr.changeMapBanish(it.map, it.portal, it.message) }
            }
            131 -> {
                monster.map?.spawnMist(Mist(calculateBoundingBox(monster.position, true), monster, this), x * 10,
                    poison = false,
                    fake = false
                )
            }
            132 -> disease = Disease.CONFUSE
            // 133 -> // Zombify
            140 -> if (makeChanceResult() && !monster.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) stats[MonsterStatus.WEAPON_IMMUNITY] = x
            141 -> if (makeChanceResult() && !monster.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) stats[MonsterStatus.MAGIC_IMMUNITY] = x
            143 -> {
                stats[MonsterStatus.WEAPON_REFLECT] = x
                stats[MonsterStatus.WEAPON_IMMUNITY] = x
            }
            144 -> {
                stats[MonsterStatus.MAGIC_REFLECT] = x
                stats[MonsterStatus.MAGIC_IMMUNITY] = x
            }
            145 -> {
                stats[MonsterStatus.WEAPON_REFLECT] = x
                stats[MonsterStatus.WEAPON_IMMUNITY] = x
                stats[MonsterStatus.MAGIC_REFLECT] = x
                stats[MonsterStatus.MAGIC_IMMUNITY] = x
            }
            //154 accuracy up 155 avoid up 156 speed up
            200 -> {
                if (monster.map?.spawnedMonsterOnMap?.let{ it.get() < 80 } == true) {
                    toSummon.forEach {
                        val toSpawn = LifeFactory.getMonster(it) ?: return@forEach
                        toSpawn.position = monster.position
                        var xPos = monster.position.x
                        var yPos = monster.position.y
                        when (it) {
                            8500003 -> { // Pap bomb high
                                toSpawn.fh = ceil(Math.random() * 19.0).toInt()
                                yPos = -590
                            }
                            8500004 -> { // Pap bomb
                                xPos = monster.position.x + Random.nextInt(1000) - 500
                                yPos = if (yPos != -590) monster.position.y else yPos
                            }
                            8510100 -> { // Pianus bomb
                                if (ceil(Math.random() * 5).toInt() == 1) {
                                    yPos = 78
                                    xPos = Random.nextInt(5) + (if (Random.nextInt(2) == 1) 180 else 0)
                                } else {
                                    xPos = monster.position.x + Random.nextInt(1000) - 500
                                }
                            }
                        }
                        when (monster.map?.mapId) {
                            220080001 -> { //Pap Map
                                if (xPos < -890) xPos = ceil(Math.random() * 150).toInt() - 890
                                else if (xPos > 230) xPos = (230 - ceil(Math.random() * 150).toInt())
                            }
                            230040420 -> { // Pianus map
                                if (xPos < -239) xPos = (ceil(Math.random() * 150) - 239).toInt()
                                else if (xPos > 371) xPos = (371 - ceil(Math.random() * 150)).toInt()
                            }
                        }
                        toSpawn.position = Point(xPos, yPos)
                        monster.map?.spawnMonsterWithEffect(toSpawn, spawnEffect, toSpawn.position)
                    }
                }
            }
        }
        if (stats.isNotEmpty()) {
            if (skill) {
                getObjectsInRange(monster, MapObjectType.MONSTER).forEach {
                    it as Monster
                    it.applyMonsterBuff(stats, x, skillId, duration, this, reflection)
                }
            } else {
                monster.applyMonsterBuff(stats, x, skillId, duration, this, reflection)
            }
        }
        if (disease != null) {
            if (skill) {
                var seduceCount = 0
                getPlayersInRange(monster, chr).forEach {
                    if (!it.isActiveBuffedValue(2321005)) {
                        if (disease == Disease.SEDUCE) {
                            if (seduceCount < 10) {
                                it.giveDebuff(Disease.SEDUCE, this)
                                seduceCount++
                            }
                        } else {
                            it.giveDebuff(disease, this)
                        }
                    }
                }
            } else {
                chr.giveDebuff(disease, this)
            }
        }
        monster.usedSkill(skillId, skillLevel, coolTime)
        monster.mp = monster.mp - mpCon
    }

    private fun calculateBoundingBox(posFrom: Point, facingLeft: Boolean): Rectangle {
        val multiplier = if (facingLeft) 1 else -1
        val myLt = Point(lt.x * multiplier + posFrom.x, lt.y + posFrom.y)
        val myRb = Point(rb.x * multiplier + posFrom.x, rb.y + posFrom.y)
        return Rectangle(myLt.x, myRb.y, myRb.x - myLt.x, myRb.y - myLt.y)
    }

    private fun getPlayersInRange(monster: Monster, chr: Character): List<Character> {
        return monster.map?.getPlayersInRange(calculateBoundingBox(monster.position, monster.isFacingLeft()), listOf(chr)) ?: listOf()
    }

    private fun getObjectsInRange(monster: Monster, objectType: MapObjectType): List<MapObject> {
        return monster.map?.getMapObjectsInBox(calculateBoundingBox(monster.position, monster.isFacingLeft()), listOf(objectType)) ?: listOf()
    }
}