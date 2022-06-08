package server.life

import client.*
import client.status.MonsterStatus
import client.status.MonsterStatusEffect
import net.server.channel.Channel
import net.server.world.Party
import scripting.event.EventInstanceManager
import server.maps.AbstractLoadedLife
import server.maps.GameMap
import server.maps.MapObjectType
import tools.CoroutineManager
import tools.PacketCreator
import tools.packet.GameplayPacket
import tools.packet.InteractPacket
import tools.packet.NpcPacket
import java.awt.Point
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class Monster : AbstractLoadedLife {
    var hp = -1
        set(value) {
            field = if (value < 0) 0 else value
        }
    var mp = -1
        set(value) {
            field = if (value < 0) 0 else value
        }
    var controller: WeakReference<Character>? = WeakReference<Character>(null)
    var controllerHasAggro = false
        set(value) {
            field = if (fake) field else value
        }
    var controllerKnowsAboutAggro = false
        set(value) {
            field = if (fake) field else value
        }
    private val attackers = mutableListOf<AttackerEntry>()
    private val listeners = mutableListOf<MonsterListener>()
    var eventInstance: EventInstanceManager? = null
    private var highestDamageChar: Character? = null
    var map: GameMap? = null
    var venomMultiplier = 0
    var fake = false
    var dropsDisabled = false
    private val usedSkills = mutableListOf<Pair<Int, Int>>()
    private val skillsUsed = mutableMapOf<Pair<Int, Int>, Int>()
    private val stolenItems = mutableListOf<Int>()
    var team = -1
    val monsterLock = ReentrantLock()
    lateinit var stats: MonsterStats
    private val stat = EnumMap<MonsterStatus, MonsterStatusEffect>(MonsterStatus::class.java)

    constructor(id: Int, stats: MonsterStats): super(id) {
        initWithStats(stats)
    }

    constructor(monster: Monster): super(monster) {
        initWithStats(monster.stats)
    }

    private fun initWithStats(stats: MonsterStats) {
        this.stats = stats
        stance = 5
        hp = stats.hp
        mp = stats.mp
    }

    /**
     * @param from the player that dealt the damage
     * @param damage
     * @param updateAttackTime
     */
    fun damage(from: Character, damage: Int, updateAttackTime: Boolean) {
        var attacker = if (from.party != null) {
            from.party?.let { PartyAttackerEntry(it.id, from.client.getChannelServer()) }
        } else {
            SingleAttackerEntry(from, from.client.getChannelServer())
        }
        var replaced = false
        for (ita in attackers) {
            if (ita is SingleAttackerEntry) {
                if (ita.chrId == (attacker as SingleAttackerEntry).chrId) {
                    replaced = true
                    attacker = ita
                    break
                }
            } else if (ita is PartyAttackerEntry) {
                if (ita.partyId == (attacker as PartyAttackerEntry).partyId) {
                    replaced = true
                    attacker = ita
                    break
                }
            }
        }
        if (!replaced) attacker?.let { attackers.add(it) }
        val rDamage = max(0, min(damage, hp))
        attacker?.addDamage(from, rDamage, updateAttackTime)
        hp -= rDamage
        var remHpPercentage = ceil((hp * 100.0) / hp)
        if (remHpPercentage < 1)
            remHpPercentage = 1.0
        val okTime = System.currentTimeMillis() - 4000
        if (hasBossHpBar()) {
            from.map.broadcastMessage(makeBossHpBarPacket(), position)
        } else if (!isBoss()) {
            attackers.forEach { m ->
                m.getAttackers().forEach { c ->
                    if (c.attacker.map == from.map) {
                        if (c.lastAttackTime >= okTime) {
                            c.attacker.client.announce(GameplayPacket.showMonsterHP(objectId, remHpPercentage.toInt()))
                        }
                    }
                }
            }
        }
    }

    fun heal(hpValue: Int, mpValue: Int) {
        var hpToHeal = hp + hpValue
        var mpToHeal = mp + mpValue
        if (hpToHeal >= getMaxHp())
            hpToHeal = getMaxHp()
        if (mpToHeal >= getMaxMp())
            mpToHeal = getMaxMp()
        hp = hpToHeal
        mp = mpToHeal
    }

    fun killBy(killer: Character): Character? {
        val totalBaseExpL = stats.exp * (killer.client.player?.expRate ?: 1)
        var totalBaseExp = min(Int.MAX_VALUE, totalBaseExpL)
        if (killer.curse)
            totalBaseExp /= 2
        var highest: AttackerEntry? = null
        var highDamage = 0
        attackers.forEach {
            if (it.getDamage() > highDamage) {
                highest = it
                highDamage = it.getDamage()
            }
        }
        attackers.forEach {
            it.killedMob(killer.map, ceil(totalBaseExp * (it.getDamage() / getMaxHp()).toDouble()).toInt(), it == highest)
        }
        controller?.get()?.client?.announce(GameplayPacket.stopControllingMonster(objectId))
        controller?.get()?.stopControllingMonster(this)
        val toSpawn = stats.revives
        val reviveMap = killer.map
        val timeMob = reviveMap.timeMob
        if (timeMob != null) {
            if (toSpawn.contains(timeMob.first)) {
                reviveMap.broadcastMessage(InteractPacket.serverNotice(6, (timeMob.second ?: 0).toString()))
            }
            if (timeMob.first == 9300338 && (reviveMap.mapId in 922240100..922240119)) {
                if (!reviveMap.containsNpc(9001108)) {
                    val npc = LifeFactory.getNpc(9001108)
                    npc.position = Point(172, 9)
                    npc.cy = 9
                    npc.rx0 = 172 + 50
                    npc.rx1 = 172 - 50
                    npc.fh = 27
                    reviveMap.addMapObject(npc)
                    reviveMap.broadcastMessage(NpcPacket.spawnNpc(npc))
                } else {
                    reviveMap.toggleHiddenNpc(9001108)
                }
            }
        }
        toSpawn.forEach { m ->
            val mob = LifeFactory.getMonster(m) ?: return@forEach
            mob.stats.revives = listOf<Int>().toMutableList()
            eventInstance?.registerMonster(mob)
            if (dropsDisabled) mob.dropsDisabled = true
            mob.position = position
            mob.stance = stance
            CoroutineManager.schedule({
                reviveMap.spawnMonster(mob, false)
            }, ((stats.getAnimationTime("die1") + 330).toLong()))
            /*CoroutineManager.schedule({
                reviveMap.spawnMonster(mob, false)
            }, (stats.getAnimationTime("die1") + 330).toLong())*/
        }
        eventInstance?.unregisterMonster(this)
        listeners.forEach {
            it.monsterKilled(this, highestDamageChar)
        }
        val ret = highestDamageChar
        highestDamageChar = null
        return ret
    }

    fun getMaxHp() = stats.hp

    private fun getMaxMp() = stats.mp

    fun giveExpToCharacter(attacker: Character, exp: Int, highestDamage: Boolean, numExpSharers: Int) {
        if (highestDamage) {
            eventInstance?.monsterKilled(attacker, this)
            highestDamageChar = attacker
        }
        if (attacker.hp > 0) {
            var personalExp = exp
            if (exp > 0) {
                val holySymbol = attacker.getBuffedValue(BuffStat.HOLY_SYMBOL)
                if (holySymbol != null) {
                    personalExp += (exp * (holySymbol.toDouble() / 100.0)).toInt()
                }
                if (stat.containsKey(MonsterStatus.SHOWDOWN)) {
                    personalExp += (exp * (stat[MonsterStatus.SHOWDOWN]?.stati?.get(MonsterStatus.SHOWDOWN)?.toDouble()
                        ?.div(100.0) ?: 1.0)).toInt()
                }
            }
            if (exp < 0)
                personalExp = Integer.MAX_VALUE
            attacker.gainExp(personalExp, show = true, inChat = false, white = highestDamage)
            attacker.mobKilled(id)
        }
    }

    fun switchController(newController: Character, immediateAggro: Boolean) {
        val controllers = controller?.get()
        if (controllers == newController) return
        controllers?.stopControllingMonster(this)
        controllers?.client?.announce(GameplayPacket.stopControllingMonster(objectId))
        newController.controlMonster(this, immediateAggro)
        controller = WeakReference(newController)
        if (immediateAggro)
            controllerHasAggro = true
        controllerKnowsAboutAggro = false
    }

    fun addListener(l: MonsterListener) = listeners.add(l)

    fun addStolen(itemId: Int) = stolenItems.add(itemId)

    fun applyStatus(from: Character, status: MonsterStatusEffect, poison: Boolean, duration: Long, venom: Boolean = false): Boolean {
        if (status.skill == null) return false
        when (status.skill.element?.let { stats.getEffectiveness(it) }) {
            ElementalEffectiveness.IMMUNE, ElementalEffectiveness.STRONG, ElementalEffectiveness.NEUTRAL -> return false
            ElementalEffectiveness.NORMAL, ElementalEffectiveness.WEAK -> {
                when (status.skill.id) {
                    2111006 -> {     // fp compo
                        val effectiveness = stats.getEffectiveness(Element.POISON)
                        if (effectiveness == ElementalEffectiveness.IMMUNE || effectiveness == ElementalEffectiveness.STRONG)
                            return false
                    }
                    2211006 -> { // il compo
                        val e = stats.getEffectiveness(Element.ICE)
                        if (e == ElementalEffectiveness.IMMUNE || e == ElementalEffectiveness.STRONG) return false
                    }
                    4120005, 4220005, 14110004 -> {// venom
                        if (stats.getEffectiveness(Element.POISON) == ElementalEffectiveness.WEAK) return false
                    }
                    else -> {}
                }
                if (poison && hp <= 1) return false
                val statIs = status.stati
                if (stats.boss) {
                    if (!(statIs.containsKey(MonsterStatus.SPEED) && statIs.containsKey(MonsterStatus.NINJA_AMBUSH)
                                && statIs.containsKey(MonsterStatus.WATK))) return false
                }
                statIs.keys.forEach {
                    val oldEffect = stat[it]
                    if (oldEffect != null) {
                        oldEffect.removeActiveStatus(it)
                        if (oldEffect.stati.isEmpty()) {
                            oldEffect.cancelTask()
                            oldEffect.cancelDamageSchedule()
                        }
                    }
                }
                val cancelTask = Runnable {
                    if (isAlive()) {
                        val packet = GameplayPacket.cancelMonsterStatus(objectId, status.stati)
                        map?.broadcastMessage(packet, position)
                        if (controller?.get()?.isMapObjectVisible(this) != true) {
                            controller?.get()?.client?.announce(packet)
                        }
                    }
                    status.stati.keys.forEach {
                        stat.remove(it)
                    }
                    venomMultiplier = 0
                    status.cancelDamageSchedule()
                }
                if (poison) {
                    val poisonLevel = from.getSkillLevel(status.skill)
                    val poisonDamage = Short.MAX_VALUE.toInt().coerceAtMost((stats.hp / (70.0 - poisonLevel) + 0.999).toInt())
                    status.setValue(MonsterStatus.POISON, poisonDamage)
                    status.damageSchedule = CoroutineManager.register(DamageTask(poisonDamage, from, status, cancelTask, 0), 1000, 1000)
                } else if (venom) {
                    if (from.job == GameJob.NIGHTLORD || from.job == GameJob.SHADOWER) {
                        val id = from.job.id
                        val skill = if (id == 412) 4120005 else if (id == 422) 4220005 else 14110004
                        val poisonLevel = SkillFactory.getSkill(skill)?.let { from.getSkillLevel(it) } ?: 0
                        if (poisonLevel <= 0) return false
                        val matk = SkillFactory.getSkill(skill)?.getEffect(poisonLevel.toInt())?.matk ?: 1
                        val luk = from.luk
                        val maxDamage = ceil(Short.MAX_VALUE.toInt().coerceAtMost((0.2 * luk * matk).toInt()).toDouble()).toInt()
                        val minDamage = ceil(Short.MAX_VALUE.toInt().coerceAtMost((0.1 * luk * matk).toInt()).toDouble()).toInt()
                        var gap = maxDamage - minDamage
                        gap = if (gap == 0) 1 else gap
                        var poisonDamage = 0
                        for (i in 0 until venomMultiplier) {
                            poisonDamage += Random.nextInt(gap) + minDamage
                        }
                        poisonDamage = Short.MAX_VALUE.coerceAtMost(poisonDamage.toShort()).toInt()
                        status.setValue(MonsterStatus.POISON, poisonDamage)
                        status.damageSchedule = CoroutineManager.register(DamageTask(poisonDamage, from, status, cancelTask, 0), 1000, 1000)
                    } else return false
                } else if (status.skill.id == 4111003 || status.skill.id == 14111001) {
                    status.damageSchedule = CoroutineManager.schedule(DamageTask((stats.hp / 50.0 + 0.999).toInt(), from, status, cancelTask, 1), 3500)
                } else if (status.skill.id == 4121004 || status.skill.id == 4221004) {
                    val skill = SkillFactory.getSkill(status.skill.id) ?: return false
                    val level = from.getSkillLevel(skill)
                    val damage = ((from.str + from.luk) * (1.5 + (level * 0.05)) * skill.getEffect(level.toInt()).damage).toInt()
                    status.setValue(MonsterStatus.NINJA_AMBUSH, damage)
                    status.damageSchedule = CoroutineManager.register(DamageTask(damage, from, status, cancelTask, 2), 1000, 1000)
                }
                status.stati.keys.forEach {
                    stat[it] = status
                }
                val animationTime = status.skill.animationTime
                val packet = GameplayPacket.applyMonsterStatus(objectId, status)
                map?.broadcastMessage(packet, position)
                if (controller?.get()?.isMapObjectVisible(this) != true) {
                    controller?.get()?.client?.announce(packet)
                }
                status.cancelTask = CoroutineManager.schedule(cancelTask, duration + animationTime)
                return true
            }
            else -> return false
        }
    }

    fun applyMonsterBuff(
        stats: Map<MonsterStatus, Int>,
        x: Int,
        skillId: Int,
        duration: Long,
        skill: MobSkill,
        reflection: List<Int?>
    ) {
        val cancelTask = Runnable {
            if (isAlive()) {
                val packet = GameplayPacket.cancelMonsterStatus(objectId, stats)
                map?.broadcastMessage(packet, position)
                if (controller?.get()?.isMapObjectVisible(this) != true) {
                    controller?.get()?.client?.announce(packet)
                }
                stats.keys.forEach { stat.remove(it) }
            }
        }
        val effect = MonsterStatusEffect(stats.toMutableMap(), null, skill, true)
        stats.keys.forEach { stat[it] = effect }
        val packet = GameplayPacket.applyMonsterStatus(objectId, effect)
        map?.broadcastMessage(packet, position)
        if (controller?.get()?.isMapObjectVisible(this) != true) {
            controller?.get()?.client?.announce(packet)
        }
        CoroutineManager.schedule(cancelTask, duration)
    }

    fun getEffectiveness(e: Element): ElementalEffectiveness {
        if (stat.size > 0 && stat[MonsterStatus.DOOM] != null) {
            return ElementalEffectiveness.NORMAL
        }
        return stats.getEffectiveness(e)
    }

    fun canUseSkill(toUse: MobSkill?): Boolean {
        if (toUse == null) return false
        usedSkills.find { it.first == toUse.skillId && it.second == toUse.skillLevel }?.let { return false }
        if (toUse.limit > 0) {
            skillsUsed[Pair(toUse.skillId, toUse.skillLevel)]?.let {
                if (it >= toUse.limit) return false
            }
        }
        if (toUse.skillId == 200) {
            val mmo = map?.mapObjects?.values ?: return false
            var i = 0
            mmo.forEach {
                if (it.objectType == MapObjectType.MONSTER) i++
            }
            if (i > 100) return false
        }
        return true
    }

    fun usedSkill(skillId: Int, level: Int, coolTime: Long) {
        usedSkills.add(Pair(skillId, level))
        val pair = Pair(skillId, level)

        skillsUsed[pair]?.let {
            val times = it + 1
            skillsUsed.remove(pair)
            skillsUsed[pair] = times
        } ?: run {
            skillsUsed[pair] = 1
        }
        CoroutineManager.schedule({
            this.clearSkill(skillId, level)
        }, coolTime)
    }

    private fun clearSkill(skillId: Int, level: Int) {
        val index = usedSkills.indexOf(usedSkills.find { it.first == skillId && it.second == level })
        if (index != -1) usedSkills.removeAt(index)
    }

    fun setTempEffectiveness(e: Element, ee: ElementalEffectiveness, milli: Long) {
        val fEE = stats.getEffectiveness(e)
        if (fEE != ElementalEffectiveness.WEAK) {
            stats.setEffectiveness(e, ee)
            CoroutineManager.schedule({
                stats.removeEffectiveness(e)
                stats.setEffectiveness(e, fEE)
            }, milli)
        }
    }

    fun isAttackedBy(chr: Character) = attackers.find { it.contains(chr) }?.let { return true } ?: false

    fun isAlive() = hp > 0

    fun isBuffed(status: MonsterStatus) = stat.containsKey(status)

    fun isControllerKnowsAboutAggro() = !fake && controllerKnowsAboutAggro

    fun hasBossHpBar() = isBoss() && stats.tagColor > 0 || isHT()

    fun hasSkill(id: Int, level: Int) = stats.hasSkill(id, level)

    fun makeBossHpBarPacket() = PacketCreator.showBossHP(id, hp, stats.hp, stats.tagColor.toByte(),
        stats.tagBgColor.toByte()
    )

    fun isBoss() = stats.boss || isHT()

    private fun isHT() = id == 8810018

    override val objectType = MapObjectType.MONSTER

    override fun sendSpawnData(client: Client) {
        if (!isAlive()) return
        if (fake) {
            client.announce(GameplayPacket.spawnFakeMonster(this, 0))
        } else {
            client.announce(GameplayPacket.spawnMonster(this, false))
        }
        val stats = this.stat
        if (stats.size > 0) {
            stats.values.forEach {
                client.announce(GameplayPacket.applyMonsterStatus(objectId, it))
            }
        }
        if (hasBossHpBar()) {
            if (map != null && map!!.countMonster(8810026) > 2 && map!!.mapId == 240060200) {
                map?.killAllMonsters()
                return
            }
            client.announce(makeBossHpBarPacket())
        }
    }

    override fun sendDestroyData(client: Client) = client.announce(GameplayPacket.killMonster(objectId, false))

    inner class DamageTask(private val dealDamage: Int, val chr: Character, val status: MonsterStatusEffect, private val cancelTask: Runnable, val type: Int) : Runnable {
        val map: GameMap = chr.map
        override fun run() {
            var damage = dealDamage
            if (damage >= hp) {
                damage = hp - 1
                if (type == 1 || type == 2) {
                    map.broadcastMessage(GameplayPacket.damageMonster(objectId, damage), position)
                    cancelTask.run()
                    status.cancelTask?.cancel()
                }
            }
        }
    }
    data class AttackingCharacter(val attacker: Character, val lastAttackTime: Long)

    interface AttackerEntry {
        fun getDamage(): Int
        fun getAttackers(): List<AttackingCharacter>
        fun addDamage(from: Character, damage: Int, updateAttackTime: Boolean)
        fun contains(chr: Character): Boolean
        fun killedMob(map: GameMap, baseExp: Int, mostDamage: Boolean)
    }

    inner class SingleAttackerEntry(val from: Character, private val chServ: Channel) : AttackerEntry {
        val chrId = from.id
        private var lastAttackTime: Long = 0
        private var damage: Int = 0

        override fun getDamage() = damage

        override fun addDamage(from: Character, damage: Int, updateAttackTime: Boolean) {
            if (chrId == from.id) this.damage += damage
            if (updateAttackTime) {
                lastAttackTime = System.currentTimeMillis()
            }
        }

        override fun getAttackers(): List<AttackingCharacter> {
            val chr = from.client.getChannelServer().players.getCharacterById(chrId)
            return chr?.let { listOf(AttackingCharacter(it, lastAttackTime)) } ?: listOf()
        }

        override fun contains(chr: Character) = chrId == chr.id

        override fun killedMob(map: GameMap, baseExp: Int, mostDamage: Boolean) {
            val chr = map.getCharacterById(chrId)
            chr?.let { giveExpToCharacter(it, baseExp, mostDamage, 1) }
        }
    }

    inner class PartyAttackerEntry(val partyId: Int, val channelServer: Channel) : AttackerEntry {
        private var totDamage = 0
        private val attackers = mutableMapOf<Int, OnePartyAttacker>()

        override fun getAttackers(): List<AttackingCharacter> {
            val ret = mutableListOf<AttackingCharacter>()
            attackers.entries.forEach { e ->
                val chr = channelServer.players.getCharacterById(e.key)
                chr?.let { ret.add(AttackingCharacter(it, e.value.lastAttackTime)) }
            }
            return ret
        }

        private fun resolveAttackers(): Map<Character, OnePartyAttacker> {
            val ret = mutableMapOf<Character, OnePartyAttacker>()
            attackers.forEach { e ->
                val chr = channelServer.players.getCharacterById(e.key)
                chr?.let { ret[chr] = e.value }
            }
            return ret
        }

        override fun contains(chr: Character) = attackers.containsKey(chr.id)

        override fun getDamage() = totDamage

        override fun addDamage(from: Character, damage: Int, updateAttackTime: Boolean) {
            val oldPartyAttacker = attackers[from.id]
            if (oldPartyAttacker != null) {
                with (oldPartyAttacker) {
                    this.damage += damage
                    this.lastKnownParty = from.party ?: return
                    if (updateAttackTime) {
                        this.lastAttackTime = System.currentTimeMillis()
                    }
                }
            } else {
                // TODO actually this causes wrong behaviour when the party changes between attacks
                // only the last setup will get exp - but otherwise we'd have to store the full party
                // constellation for every attack/everytime it changes, might be wanted/needed in the
                // future but not now
                val onePartyAttacker = from.party?.let { OnePartyAttacker(it, damage) } ?: return
                attackers[from.id] = onePartyAttacker
                if (!updateAttackTime) {
                    onePartyAttacker.lastAttackTime = 0
                }
            }
            totDamage += damage
        }

        override fun killedMob(map: GameMap, baseExp: Int, mostDamage: Boolean) {
            val resolveAttackers = resolveAttackers()
            var highest: Character? = null
            var highestDamage = 0
            val expMap = mutableMapOf<Character, Int>()
            resolveAttackers.forEach { e ->
                val party = e.value.lastKnownParty
                var averagePartyLevel = 0
                val expApplicable = mutableListOf<Character>()
                party.members.forEach { m ->
                    if (m.level != null && (e.key.level - m.level <= 5 || stats.level - m.level <= 5)) {
                        val pChr = m.name?.let { channelServer.players.getCharacterByName(it) }
                        pChr?.let {
                            if (it.isAlive() && it.map == map) {
                                expApplicable.add(it)
                                averagePartyLevel += it.level
                            }
                        }
                    }
                }
                var expBonus = 1.0
                if (expApplicable.size > 1) {
                    expBonus = 1.10 + 0.05 * expApplicable.size
                    averagePartyLevel /= expApplicable.size
                }
                val iDamage = e.value.damage
                if (iDamage > highestDamage) {
                    highest = e.key
                    highestDamage = iDamage
                }
                val innerBaseExp = baseExp * (iDamage / totDamage.toDouble())
                val expFraction = (innerBaseExp * expBonus) / (expApplicable.size + 1)
                expApplicable.forEach {
                    var exp = expMap[it] ?: 0
                    val expWeight = if (it == e.key) 2.0 else 1.0
                    var levelMod = it.level / averagePartyLevel.toDouble()
                    if (levelMod > 1.0 || attackers.containsKey(it.id)) {
                        levelMod = 1.0
                    }
                    exp += (expFraction * expWeight * levelMod).roundToInt()
                    expMap[it] = exp
                }
            }
            expMap.forEach {
                giveExpToCharacter(it.key, it.value, mostDamage && it.key == highest, expMap.size)
            }
        }
    }

    companion object {
        data class OnePartyAttacker(var lastKnownParty: Party, var damage: Int) {
            var lastAttackTime = System.currentTimeMillis()
        }
    }
}