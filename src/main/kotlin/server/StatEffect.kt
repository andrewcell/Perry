package server

import client.*
import client.inventory.InventoryType
import client.status.MonsterStatus
import client.status.MonsterStatusEffect
import constants.ItemConstants
import constants.skills.*
import net.server.Server
import provider.Data
import provider.DataTool
import server.life.Monster
import server.maps.*
import tools.CoroutineManager
import tools.PacketCreator
import tools.packet.CharacterPacket
import tools.packet.GameplayPacket
import java.awt.Point
import java.awt.Rectangle
import java.lang.ref.WeakReference

class StatEffect(
    var duration: Int,
    val hp: Int,
    var hpR: Double,
    val mp: Int,
    val mpR: Double,
    val hpCon: Short,
    val mpCon: Short,
    val prop: Double,
    val mobCount: Int,
    val morphId: Int,
    val ghost: Int,
    val fatigue: Int,
    val repeatEffect: Boolean,
    val sourceId: Int,
    val skill: Boolean,
    val watk: Short,
    val wdef: Short,
    val matk: Short,
    val mdef: Short,
    val acc: Short,
    val avoid: Short,
    val speed: Short,
    val jump: Short,
    val berserk: Int,
    val booster: Int,
    val overTime: Boolean,
    val x: Int,
    val y: Int,
    val damage: Int,
    val fixDamage: Int,
    val attackCount: Int,
    val bulletCount: Byte,
    val bulletConsume: Byte,
    val moneyCon: Int,
    val itemCon: Int,
    val itemConNo: Int,
    val moveTo: Int
) {
    private var world = 0
    var statUps: List<Pair<BuffStat, Int>>? = null
    var monsterStatus: Map<MonsterStatus, Int>? = null
    var lt: Point? = null
    var rb: Point? = null

    private fun isMorph() = morphId > 0

    fun isBeholder() = skill && sourceId == DarkKnight.BEHOLDER

    fun isBerserk() = skill && sourceId == DarkKnight.BERSERK

    private fun isChakra() = skill && sourceId == ChiefBandit.CHAKRA

    private fun isDash(): Boolean {
        if (!skill) return false
        return when (sourceId) {
            Pirate.DASH, Beginner.SPACE_DASH -> true
            else -> false
        }
    }

    private fun isDispel() = skill && (sourceId == Priest.DISPEL || sourceId == GM.HEAL_PLUS_DISPEL || sourceId == SuperGM.HEAL_PLUS_DISPEL)

    fun isDragonBlood() = skill && sourceId == DragonKnight.DRAGON_BLOOD

    private fun isDs(): Boolean {
        if (!skill) return false
        return when (sourceId) {
            Rogue.DARK_SIGHT -> true
            else -> false
        }
    }

    private fun isEnrage() = skill && sourceId == Hero.ENRAGE

    private fun isGmBuff() = when(sourceId) {
        Beginner.ECHO_OF_HERO, GM.HEAL_PLUS_DISPEL, GM.HASTE, GM.HOLY_SYMBOL, GM.BLESS, GM.RESURRECTION, SuperGM.HEAL_PLUS_DISPEL, SuperGM.HASTE, SuperGM.HOLY_SYMBOL, SuperGM.BLESS, SuperGM.RESURRECTION, SuperGM.HYPER_BODY -> true
        else -> false
    }

    private fun isInfusion() = skill && (sourceId == Corsair.SPEED_INFUSION || sourceId == Buccaneer.SPEED_INFUSION)

    fun isMagicDoor() = skill && sourceId == Priest.MYSTIC_DOOR

    private fun isMist(): Boolean {
        if (!skill) return false
        return when (sourceId) {
            FPMage.POISON_MIST, Shadower.SMOKE_SCREEN -> true
            else -> false
        }
    }

    fun isMonsterRiding(): Boolean {
        if (!skill) return false
        if (sourceId % 10000000 == 1004) return true
        return when (sourceId) {
            Corsair.BATTLE_SHIP, Beginner.SPACESHIP, Beginner.YETI_MOUNT1, Beginner.YETI_MOUNT2, Beginner.WITCH_BROOMSTICK, Beginner.BALROG_MOUNT -> true
            else -> false
        }
    }

    private fun isPartyBuff(): Boolean {
        if (lt == null || rb == null) return false
        if (sourceId in 1211003..1211008) return false
        return when (sourceId) {
            Paladin.SWORD_HOLY_CHARGE, Paladin.BW_HOLY_CHARGE, DawnWarrior.SOUL_CHARGE -> false
            else -> true
        }
    }

    fun isPoison() = skill && when (sourceId) {
        FPMage.POISON_MIST, FPWizard.POISON_BREATH, FPMage.ELEMENT_COMPOSITION -> true
        else -> false
    }

    fun isRecovery() = sourceId == Beginner.RECOVERY

    private fun isResurrection(): Boolean {
        return when (sourceId) {
            Bishop.RESURRECTION, GM.RESURRECTION, SuperGM.RESURRECTION -> true
            else -> false
        }
    }

    private fun isShadowPartner() = skill && sourceId == Hermit.SHADOW_PARTNER

    private fun isSoulArrow() = skill && (sourceId == Hunter.SOUL_ARROW || sourceId == Crossbowman.SOUL_ARROW)

    private fun isTimeLeap() = sourceId == Buccaneer.TIME_LEAP

    private fun isHeal(): Boolean {
        return when (sourceId) {
            Cleric.HEAL, GM.HEAL_PLUS_DISPEL, SuperGM.HEAL_PLUS_DISPEL -> true
            else -> false
        }
    }


    private fun isHeroWill(): Boolean {
        if (skill) {
            return when (sourceId) {
                Hero.HEROS_WILL, Paladin.HEROS_WILL, DarkKnight.HEROS_WILL, FPArchMage.HEROS_WILL, ILArchMage.HEROS_WILL, Bishop.HEROS_WILL, Bowmaster.HEROS_WILL, Marksman.HEROS_WILL, NightLord.HEROS_WILL, Shadower.HEROS_WILL, Buccaneer.PIRATES_RAGE -> true
                else -> false
            }
        }
        return false
    }

    private fun isShadowClaw() = skill && sourceId == NightLord.SHADOW_STARS

    private fun isMonsterBuff(): Boolean {
        if (!skill) return false
        return when (sourceId) {
            Page.THREATEN, FPWizard.SLOW, ILWizard.SLOW, FPMage.SEAL, ILMage.SEAL, Priest.DOOM, Hermit.SHADOW_WEB, NightLord.NINJA_AMBUSH, Shadower.NINJA_AMBUSH -> return true
            else -> false
        }
    }

    fun sameSource(effect: StatEffect) = sourceId == effect.sourceId && skill == effect.skill

    fun makeChanceResult() = prop == 1.0 || Math.random() < prop

    private fun getMorph(chr: Character): Int = if (morphId % 10 == 0) morphId + chr.gender else morphId + 100 * chr.gender

    private fun getSummonMovementType(): SummonMovementType? {
        if (!skill) return null
        return when (sourceId) {
            Ranger.PUPPET, Sniper.PUPPET, Outlaw.OCTOPUS, Corsair.WRATH_OF_THE_OCTOPI -> SummonMovementType.STATIONARY
            Ranger.SILVER_HAWK, Sniper.GOLDEN_EAGLE, Priest.SUMMON_DRAGON, Marksman.FROST_PREY, Bowmaster.PHOENIX, Outlaw.GAVIOTA -> SummonMovementType.CIRCLE_FOLLOW
            DarkKnight.BEHOLDER, FPArchMage.ELQUINES, ILArchMage.IFRIT, Bishop.BAHAMUT, DawnWarrior.SOUL -> SummonMovementType.FOLLOW
            else -> null
        }
    }

    private fun getAlchemistEffect(chr: Character): StatEffect? {
        val id = Hermit.ALCHEMIST
        val alchemistLevel = SkillFactory.getSkill(id)?.let { chr.getSkillLevel(it) } ?: return null
        return if (alchemistLevel.toInt() == 0) null else SkillFactory.getSkill(id)?.getEffect(alchemistLevel.toInt())
    }

    private fun alchemistModifyVal(chr: Character, value: Int, withX: Boolean): Int {
        if (!skill && (chr.job.isA(GameJob.HERMIT))) {
            val alchemistEffect = getAlchemistEffect(chr)
            if (alchemistEffect != null) {
                return (value * ((if (withX) alchemistEffect.x else alchemistEffect.y) / 100.0)).toInt()
            }
        }
        return value
    }

    private fun makeHealHp(rate: Double, stat: Double, lowerFactor: Double, upperFactor: Double): Int {
        return (Math.random() * ((stat * upperFactor * rate)- (stat * lowerFactor * rate) + 1) + stat * lowerFactor * rate).toInt()
    }

    private fun calcHpChange(applyFrom: Character, primary: Boolean): Int {
        var hpChange = 0
        if (hp != 0) {
            hpChange += if (!skill) {
                if (primary) {
                    alchemistModifyVal(applyFrom, hp, true)
                } else {
                    hp
                }
            } else {
                makeHealHp(hp / 100.0, applyFrom.magic.toDouble(), 3.0, 5.0)
            }
        }
        if (hpR != 0.0) {
            hpChange += applyFrom.localMaxHp * hpR.toInt()
            applyFrom.checkBerserk()
        }
        if (primary) {
            if (hpCon != 0.toShort()) {
                hpChange -= hpCon
            }
        }
        if (isChakra()) {
            hpChange += makeHealHp(y / 100.0, applyFrom.localLuk.toDouble(), 2.3, 3.5)
        } else if (sourceId == GM.HEAL_PLUS_DISPEL || sourceId == SuperGM.HEAL_PLUS_DISPEL) {
            hpChange += applyFrom.maxHp - applyFrom.hp
        }
        return hpChange
    }

    private fun calcMpChange(applyFrom: Character, primary: Boolean, buff: Boolean): Int {
        var mpChange = 0
        if (mp != 0) {
            mpChange += if (primary) {
                alchemistModifyVal(applyFrom, mp, true)
            } else {
                mp
            }
        }
        if (mpR != 0.0) {
            mpChange += (applyFrom.localMaxMp * mpR).toInt()
        }
        if (primary) {
            if (mpCon.toInt() != 0) {
                var mod = 1.0
                if (!buff) {
                    val isAFpMage = applyFrom.job.isA(GameJob.FP_MAGE)
                    if (isAFpMage) {
                        val amp = if (isAFpMage) SkillFactory.getSkill(FPMage.ELEMENT_AMPLIFICATION) else SkillFactory.getSkill(ILMage.ELEMENT_AMPLIFICATION)
                        val ampLevel = amp?.let { applyFrom.getSkillLevel(it) } ?: return 0
                        if (ampLevel > 0) {
                            mod = amp.getEffect(ampLevel.toInt()).x / 100.0
                        }
                    }
                }
                mpChange -= (mpCon * mod).toInt()
                if (applyFrom.getBuffedValue(BuffStat.INFINITY) != null) {
                    mpChange = 0
                } else if (applyFrom.getBuffedValue(BuffStat.CONCENTRATE) != null) {
                    mpChange -= (mpChange * ((applyFrom.getBuffedValue(BuffStat.CONCENTRATE) ?: 1).toDouble() / 100)).toInt()

                }
            }
        }
        if (sourceId == GM.HEAL_PLUS_DISPEL || sourceId == SuperGM.HEAL_PLUS_DISPEL) {
            mpChange += applyFrom.maxMp - applyFrom.mp
        }
        return mpChange
    }

    private fun calculateBoundingBox(posFrom: Point, facingLeft: Boolean): Rectangle {
        val myLt: Point
        val myRb: Point
        if (facingLeft) {
            myLt = Point(lt!!.x + posFrom.x, lt!!.y + posFrom.y)
            myRb = Point(rb!!.x + posFrom.x, rb!!.y + posFrom.y)
        } else {
            myRb = Point(-lt!!.x + posFrom.x, rb!!.y + posFrom.y)
            myLt = Point(-rb!!.x + posFrom.x, lt!!.y + posFrom.y)
        }
        return Rectangle(myLt.x, myLt.y, myRb.x - myLt.x, myRb.y - myLt.y)
    }
    /**
     *
     * @param applyTo
     * @param attack damage done by the skill
     */
    fun applyPassive(applyTo: Character, obj: MapObject, attack: Int) {
        if (makeChanceResult()) {
            when (sourceId) {
                FPWizard.MP_EATER, ILWizard.MP_EATER, Cleric.MP_EATER -> {
                    if (obj.objectType != MapObjectType.MONSTER) return
                    val mob = obj as Monster
                    if (!mob.isBoss()) {
                        val absorbMp = (mob.stats.mp * (x / 100.0)).toInt().coerceAtMost(mob.mp)
                        if (absorbMp > 0) {
                            mob.mp = mob.mp - absorbMp
                            applyTo.addMp(absorbMp)
                            applyTo.client.announce(CharacterPacket.showOwnBuffEffect(sourceId, 1))
                            applyTo.map.broadcastMessage(applyTo, PacketCreator.showBuffEffect(applyTo.id, sourceId, 1), false)
                        }
                    }
                }
            }
        }
    }

    fun applyTo(chr: Character): Boolean = applyTo(chr, chr, primary = true, buff = false, pos = null)

    fun applyTo(chr: Character, pos: Point?) = applyTo(chr, chr, primary = true, buff = true, pos = pos)

    fun applyTo(applyFrom: Character, applyTo: Character, primary: Boolean, buff: Boolean, pos: Point?): Boolean {
        if (skill && sourceId == GM.HIDE && applyTo.isGM()) {
            val statUp = mutableListOf(Pair(BuffStat.DARKSIGHT, 1))
            applyTo.announce(CharacterPacket.giveBuff(sourceId, 0, statUp))
            applyTo.toggleHidden()
            return true
        }
        var hpChange = calcHpChange(applyFrom, primary)
        val mpChange = calcMpChange(applyFrom, primary, buff)
        if (primary) {
            if (itemConNo != 0) {
                InventoryManipulator.removeById(applyTo.client, ItemInformationProvider.getInventoryType(itemCon), itemCon, itemConNo,
                    fromDrop = false,
                    consume = true
                )
            }
        }
        val hpMpUpdate = mutableListOf<Pair<CharacterStat, Int>>()
        if (!primary && isResurrection()) {
            hpChange = applyTo.maxHp
            applyTo.stance = 0
            applyTo.map.broadcastMessage(applyTo, GameplayPacket.removePlayerFromMap(applyTo.id))
            applyTo.map.broadcastMessage(applyTo, GameplayPacket.spawnPlayerMapObject(applyTo))
            applyTo.updatePartyMemberHp()
        }
        if (isDispel() && makeChanceResult()) {
            applyTo.dispelDeBuffs()
        } else if (isHeroWill()) {
            applyTo.dispelDeBuff(Disease.SEDUCE)
        }
        if (hpChange != 0) {
            if (hpChange < 0 && (-hpChange) > applyTo.hp) {
                return false
            }
            var newHp = applyTo.hp + hpChange
            if (newHp < 1) newHp = 1
            applyTo.setHpNormal(newHp)
            if (isHeal() && applyFrom.party != null) {
                val channel = applyFrom.client.channel
                applyFrom.party?.members?.forEach { partyMember ->
                    if (partyMember.mapId == applyFrom.mapId && partyMember.channel == channel) {
                        val other = partyMember.name?.let { Server.worlds[world].getChannel(channel).players.getCharacterByName(it) }
                        if (other != null) {
                            applyFrom.gainExp((other.localMaxHp - other.hp) / 100 * 7, show = false, inChat = false)
                        }
                    }
                }
            }
            hpMpUpdate.add(Pair(CharacterStat.HP, applyTo.hp))
        }
        val newMp = applyTo.mp + mpChange
        if (mpChange != 0) {
            if (mpChange < 0 && -mpChange > applyTo.mp) {
                return false
            }
            applyTo.setMpNormal(newMp)
            hpMpUpdate.add(Pair(CharacterStat.MP, applyTo.mp))
        }
        applyTo.client.announce(CharacterPacket.updatePlayerStats(hpMpUpdate, true))
        if (moveTo != -1) {
            val target: GameMap
            if (moveTo == 999999999) {
                if (applyTo.map.returnMapId != applyTo.mapId) {
                    target = applyTo.map.getReturnMap()
                } else {
                    return false
                }
            } else {
                target = applyTo.client.getWorldServer().getChannel(applyTo.client.channel).mapFactory.getMap(moveTo)
                if (target.returnMapId != applyTo.mapId) {
                    val targetId = target.mapId / 10000000
                    if (targetId != 60 && applyTo.mapId / 10000000 != 61 && targetId != applyTo.mapId / 10000000 && targetId != 21 && targetId != 20) {
                        return false
                    }
                } else {
                    return false
                }
            }
        }
        if (isShadowClaw()) {
            var projectile = 0
            val use = applyTo.getInventory(InventoryType.USE)
            for (i in 0..96) {
                val item = use?.getItem(i.toByte())
                if (item != null) {
                    if (ItemConstants.isThrowingStar(item.itemId) && item.quantity >= 200) {
                        projectile = item.itemId
                        break
                    }
                }
            }
            if (projectile == 0) {
                return false
            } else {
                InventoryManipulator.removeById(applyTo.client, InventoryType.USE, projectile, 200,
                    fromDrop = false,
                    consume = true
                )
            }
        }

        val summonMovementType = getSummonMovementType()
        if (overTime || summonMovementType != null) {
            applyBuffEffect(applyFrom, applyTo, primary)
        }

        if (primary && (overTime || isHeal())) {
            applyBuff(applyFrom)
            if (skill && sourceId == Spearman.HYPER_BODY || sourceId == SuperGM.HYPER_BODY) {
                applyFrom.setBuffedValue(BuffStat.HYPERBODYHP, x)
                applyFrom.setBuffedValue(BuffStat.HYPERBODYMP, y)
                applyFrom.recalcLocalStats()
            }
        }

        if (primary && isMonsterBuff()) {
            applyMonsterBuff(applyFrom)
        }

        if (fatigue != 0) {
            applyTo.mount?.tiredness = applyTo.mount?.tiredness?.plus(fatigue) ?: 1
        }

        if (summonMovementType != null && pos != null) {
            val toSummon = Summon(applyFrom, sourceId, pos, summonMovementType)
            applyFrom.map.spawnSummon(toSummon)
            applyFrom.addSummon(sourceId, toSummon)
            toSummon.addHp(x)
            if (isBeholder()) toSummon.addHp(1)
        }
        if (isMagicDoor() && !FieldLimit.DOOR.check(applyTo.map.fieldLimit)) {
            val doorPos = Point(applyTo.position)
            var door = Door(applyTo, doorPos)
            applyTo.map.spawnDoor(door)
            applyTo.addDoor(door)
            door = Door(door.owner, door.targetPosition)
            applyTo.addDoor(door)
            door.town.spawnDoor(door)
            if (applyTo.party != null) {
                applyTo.silentPartyUpdate()
            }
            applyTo.disableDoor()
        } else if (isMist()) {
            val bounds = calculateBoundingBox(applyFrom.position, applyFrom.isFacingLeft())
            val mist = Mist(bounds, applyFrom, this)
            applyFrom.map.spawnMist(mist, duration, sourceId != Shadower.SMOKE_SCREEN, false)
        } else if (isTimeLeap()) {
            applyTo.getAllCoolDowns().forEach { coolDown ->
                if (coolDown.skillId != Buccaneer.TIME_LEAP) {
                    applyTo.removeCoolDown(coolDown.skillId)
                }
            }
        }
        return true
    }

    private fun applyBuff(applyFrom: Character) {
        if (isPartyBuff() && (applyFrom.party != null || isGmBuff())) {
            val bounds = calculateBoundingBox(applyFrom.position, applyFrom.isFacingLeft())
            val affectedObjects = applyFrom.map.getMapObjectsInRect(bounds, listOf(MapObjectType.PLAYER))
            val affectedPlayers = mutableListOf<Character>()
            affectedObjects.forEach { affectedMapObject ->
                val affected = affectedMapObject as Character
                if (affected != applyFrom && (isGmBuff() || applyFrom.party == affected.party)) {
                    if ((isResurrection() && !affected.isAlive()) || (!isResurrection() && affected.isAlive())) {
                        affectedPlayers.add(affected)
                    }
                    if (isTimeLeap()) {
                        affected.getAllCoolDowns().forEach { coolDown ->
                            affected.removeCoolDown(coolDown.skillId)
                        }
                    }
                }
            }
            affectedPlayers.forEach { affected ->
                applyTo(applyFrom, affected, primary = false, buff = false, pos = null)
                affected.client.announce(CharacterPacket.showOwnBuffEffect(sourceId, 2))
                affected.map.broadcastMessage(affected, PacketCreator.showBuffEffect(affected.id, sourceId, 2), false)
                if (skill && sourceId == Spearman.HYPER_BODY || sourceId == SuperGM.HYPER_BODY) {
                    affected.setBuffedValue(BuffStat.HYPERBODYHP, x)
                    affected.setBuffedValue(BuffStat.HYPERBODYMP, y)
                    affected.recalcLocalStats()
                }
            }
        }
    }

    private fun applyMonsterBuff(applyFrom: Character) {
        val bounds = calculateBoundingBox(applyFrom.position, applyFrom.isFacingLeft())
        val affected = applyFrom.map.getMapObjectsInRect(bounds, listOf(MapObjectType.MONSTER))
        val skill1 = SkillFactory.getSkill(sourceId)
        run loop@ {
            affected.forEachIndexed { index, mapObject ->
                val monster = mapObject as Monster
                if (makeChanceResult()) {
                    monster.applyStatus(applyFrom,
                        MonsterStatusEffect((monsterStatus?.toMutableMap() ?: mutableMapOf()), skill1, null, false),
                        isPoison(),
                        duration.toLong()
                    )
                }
                if (index >= mobCount) {
                    return@loop
                }
            }
        }
    }

    private fun applyBuffEffect(applyFrom: Character, applyTo: Character, primary: Boolean) {
        if (!isMonsterRiding()) {
            applyTo.cancelEffect(this, true, -1)
        }

        val localStatUps = statUps
        var localDuration = duration
        var localSourceId = sourceId
        val seconds = localDuration / 1000
        var giveMount: Mount? = null
        if (isMonsterRiding()) {
            var ridingLevel = 0
            val mount = applyFrom.getInventory(InventoryType.EQUIPPED)?.getItem(-18)
            if (mount != null) {
                ridingLevel = mount.itemId
            }
            ridingLevel = when (sourceId) {
                Corsair.BATTLE_SHIP -> 1932000
                Beginner.SPACESHIP -> 1932000 + applyTo.getSkillLevel(sourceId)
                Beginner.YETI_MOUNT1 -> 1932003
                Beginner.YETI_MOUNT2 -> 1932004
                Beginner.WITCH_BROOMSTICK -> 1932005
                Beginner.BALROG_MOUNT -> 1932010
                else -> {
                    if (applyTo.mount == null) applyTo.mount(ridingLevel, sourceId)
                    applyTo.mount?.startSchedule()
                    ridingLevel
                }
            }
            giveMount = when (sourceId) {
                Corsair.BATTLE_SHIP, Beginner.SPACESHIP, Beginner.YETI_MOUNT1, Beginner.YETI_MOUNT2, Beginner.WITCH_BROOMSTICK, Beginner.BALROG_MOUNT-> Mount(applyTo, sourceId, sourceId)
                else -> applyTo.mount
            }
            localDuration = sourceId
            localSourceId = ridingLevel
        }
        if (primary) {
            localDuration = alchemistModifyVal(applyFrom, localDuration, false)
            applyTo.map.broadcastMessage(applyTo, PacketCreator.showBuffEffect(applyTo.id, sourceId, 1, 3), false)
        }
        var buff: ByteArray? = null
        var mBuff: ByteArray? = null
        if (getSummonMovementType() == null) {
            buff = CharacterPacket.giveBuff(if (skill) sourceId else -sourceId, localDuration, localStatUps ?: emptyList())
        }
        if (localStatUps != null) {
            if (localStatUps.isNotEmpty()) {
                when {
                    isDash() -> {
                        buff = CharacterPacket.givePirateBuff(statUps ?: emptyList(), sourceId, seconds)
                        mBuff = CharacterPacket.giveForeignDash(applyTo.id, sourceId, seconds, localStatUps)
                    }
                    isInfusion() -> {
                        buff = CharacterPacket.givePirateBuff(statUps ?: emptyList(), sourceId, seconds)
                        mBuff = CharacterPacket.giveForeignInfusion(applyTo.id, x, localDuration)
                    }
                    isDs() -> {
                        val dsStat = listOf(Pair(BuffStat.DARKSIGHT, 0))
                        mBuff = CharacterPacket.giveForeignBuff(applyTo.id, dsStat)
                    }
                    isMonsterRiding() -> {
                        buff = CharacterPacket.giveBuff(localSourceId, localDuration, localStatUps)
                        mBuff = CharacterPacket.showMonsterRiding(applyTo.id, giveMount)
                        localDuration = duration
                        if (sourceId == Corsair.BATTLE_SHIP) {
                            if (applyTo.battleshipHp == 0) {
                                applyTo.resetBattleshipHp()
                            }
                        }
                    }
                    isShadowPartner() -> {
                        val stat = listOf(Pair(BuffStat.SHADOWPARTNER, 0))
                        mBuff = CharacterPacket.giveForeignBuff(applyTo.id, stat)
                    }
                    isSoulArrow() -> {
                        val stat = listOf(Pair(BuffStat.SOULARROW, 0))
                        mBuff = CharacterPacket.giveForeignBuff(applyTo.id, stat)
                    }
                    isEnrage() -> {
                        applyTo.handleOrbconsume()
                    }
                    isMorph() -> {
                        val stat = listOf(Pair(BuffStat.MORPH, getMorph(applyTo)))
                        mBuff = CharacterPacket.giveForeignBuff(applyTo.id, stat)
                    }
                    isTimeLeap() -> {
                        applyTo.getAllCoolDowns().forEach { coolDown ->
                            if (coolDown.skillId != Buccaneer.TIME_LEAP) {
                                applyTo.removeCoolDown(coolDown.skillId)
                            }
                        }
                    }
                }
            }
        }
        val startTime = System.currentTimeMillis()
        val cancelAction = CancelEffectAction(WeakReference(applyTo), this, startTime)
        val schedule = CoroutineManager.schedule(cancelAction, localDuration.toLong())
        if (schedule != null) {
            applyTo.registerEffect(this, startTime, schedule)
        }
        if (buff != null) applyTo.client.announce(buff)
        if (mBuff != null) applyTo.map.broadcastMessage(applyTo, mBuff, false)
        if (sourceId == Corsair.BATTLE_SHIP) applyTo.announce(CharacterPacket.skillCoolDown(5221999, applyTo.battleshipHp / 10))

    }

    fun silentApplyBuff(chr: Character, startTime: Long) {
        var localDuration = duration
        localDuration = alchemistModifyVal(chr, localDuration, false)
        val cancelAction = CancelEffectAction(WeakReference(chr), this, startTime)
        val schedule = CoroutineManager.schedule(cancelAction, ((startTime + localDuration) - System.currentTimeMillis()))
        schedule?.let { chr.registerEffect(this, startTime, it) }
        val summonMovementType = getSummonMovementType()
        if (summonMovementType != null) {
            val toSummon = Summon(chr, sourceId, chr.position, summonMovementType)
            if (!toSummon.isStationary()) {
                chr.addSummon(sourceId, toSummon)
                toSummon.addHp(x)
            }
        }
        if (sourceId == Corsair.BATTLE_SHIP) {
            chr.announce(CharacterPacket.skillCoolDown(5221999, chr.battleshipHp))
        }
    }

    companion object {
        class CancelEffectAction(val target: WeakReference<Character>, val effect: StatEffect, private val startTime: Long) : Runnable {
            override fun run() {
                val realTarget = target.get()
                realTarget?.cancelEffect(effect, false, startTime)
            }
        }

        private fun loadFromData(source: Data?, sourceId: Int, skill: Boolean, overTime: Boolean): StatEffect {
            var duration = DataTool.getIntConvert("time", source, -1)
            var overTimeModified = true
            val x = DataTool.getInt("x", source, 0)
            val iProp = DataTool.getInt("prop", source, 100) / 100.0

            if (!(!skill && duration > -1)) {
                duration *= 1000
                overTimeModified = overTime
            }

            val ret = StatEffect(
                duration = duration,
                hp = DataTool.getInt("hp", source, 0),
                hpR = DataTool.getInt("hpR", source, 0) / 100.0,
                mp = DataTool.getInt("mp", source, 0),
                mpR = DataTool.getInt("mpR", source, 0) / 100.0,
                hpCon = DataTool.getInt("hpCon", source, 0).toShort(),
                mpCon = DataTool.getInt("mpCon", source, 0).toShort(),
                prop = iProp,
                mobCount = DataTool.getInt("mobCount", source, 1),
                morphId = DataTool.getInt("morph", source, 0),
                ghost = DataTool.getInt("ghost", source, 0),
                fatigue = DataTool.getInt("incFatigue", source, 0),
                repeatEffect = DataTool.getInt("repeatEffect", source, 0) > 0,
                sourceId = sourceId,
                skill = skill,
                watk = DataTool.getInt("pad", source, 0).toShort(),
                wdef = DataTool.getInt("pdd", source, 0).toShort(),
                matk = DataTool.getInt("mad", source, 0).toShort(),
                mdef = DataTool.getInt("mdd", source, 0).toShort(),
                acc = DataTool.getIntConvert("acc", source, 0).toShort(),
                avoid = DataTool.getInt("jump", source ,0).toShort(),
                speed = DataTool.getInt("speed", source, 0).toShort(),
                jump = DataTool.getInt("jump", source, 0).toShort(),
                berserk = DataTool.getInt("berserk", source, 0),
                booster = DataTool.getInt("booster", source, 0),
                overTime = overTimeModified,
                x = x,
                y = DataTool.getInt("y", source, 0),
                damage = DataTool.getIntConvert("damage", source, 100),
                fixDamage = DataTool.getIntConvert("fixdamage", source, -1),
                attackCount = DataTool.getIntConvert("attackCount", source, 1),
                bulletCount = DataTool.getIntConvert("bulletCount", source, 1).toByte(),
                bulletConsume = DataTool.getIntConvert("bulletConsume", source, 0).toByte(),
                moneyCon = DataTool.getIntConvert("moneyCon", source, 0),
                itemCon = DataTool.getInt("itemCon", source, 0),
                itemConNo = DataTool.getInt("itemConNo", source, 0),
                moveTo = DataTool.getInt("moveTo", source, -1)
            )
            val statUps = mutableListOf<Pair<BuffStat, Int>>()
            fun addBuffStatPairToListIfNotZero(buffStat: BuffStat, value: Int){
                if (value != 0) statUps.add(Pair(buffStat, value))
            }
            if (ret.overTime && ret.getSummonMovementType() ==  null) {
                addBuffStatPairToListIfNotZero(BuffStat.WATK, ret.watk.toInt())
                addBuffStatPairToListIfNotZero(BuffStat.WDEF, ret.wdef.toInt())
                addBuffStatPairToListIfNotZero(BuffStat.MATK, ret.matk.toInt())
                addBuffStatPairToListIfNotZero(BuffStat.MDEF, ret.mdef.toInt())
                addBuffStatPairToListIfNotZero(BuffStat.ACC, ret.acc.toInt())
                addBuffStatPairToListIfNotZero(BuffStat.SPEED, ret.avoid.toInt())
                addBuffStatPairToListIfNotZero(BuffStat.JUMP, ret.speed.toInt())
                addBuffStatPairToListIfNotZero(BuffStat.PYRAMID_PQ, ret.jump.toInt())
                addBuffStatPairToListIfNotZero(BuffStat.BOOSTER, ret.booster)
            }
            val ltd = source?.getChildByPath("lt")
            if (ltd != null) {
                ret.lt = ltd.data as Point
                ret.rb = source.getChildByPath("rb")?.data as Point
            }
            val monsterStatus = mutableMapOf<MonsterStatus, Int>()
            if (skill) {
                when (sourceId) {
                    Beginner.RECOVERY -> statUps.add(Pair(BuffStat.RECOVERY, Integer.valueOf(x)))
                    Beginner.ECHO_OF_HERO -> statUps.add(Pair(BuffStat.ECHO_OF_HERO, Integer.valueOf(ret.x)))
                    Beginner.MONSTER_RIDER, Corsair.BATTLE_SHIP, Beginner.SPACESHIP, Beginner.YETI_MOUNT1, Beginner.YETI_MOUNT2, Beginner.WITCH_BROOMSTICK,  Beginner.BALROG_MOUNT -> statUps.add(
                        Pair(BuffStat.MONSTER_RIDING, sourceId)
                    )
                    Beginner.BERSERK_FURY -> statUps.add(
                        Pair(BuffStat.BERSERK_FURY, 1)
                    )
                    Beginner.INVINCIBLE_BARRIER -> statUps.add(
                        Pair(BuffStat.DIVINE_BODY, 1)
                    )
                    Fighter.POWER_GUARD, Page.POWER_GUARD -> statUps.add(
                        Pair(BuffStat.POWERGUARD, x)
                    )
                    Spearman.HYPER_BODY, SuperGM.HYPER_BODY -> {
                        statUps.add(Pair(BuffStat.HYPERBODYHP, x))
                        statUps.add(Pair(BuffStat.HYPERBODYMP, ret.y))
                    }
                    Crusader.COMBO, DawnWarrior.COMBO -> statUps.add(Pair(BuffStat.COMBO, 1))
                    WhiteKnight.BW_FIRE_CHARGE, WhiteKnight.BW_ICE_CHARGE, WhiteKnight.BW_LIT_CHARGE, WhiteKnight.SWORD_FIRE_CHARGE, WhiteKnight.SWORD_ICE_CHARGE, WhiteKnight.SWORD_LIT_CHARGE, Paladin.BW_HOLY_CHARGE, Paladin.SWORD_HOLY_CHARGE, DawnWarrior.SOUL_CHARGE -> statUps.add(
                        Pair(BuffStat.WK_CHARGE, x)
                    )
                    DragonKnight.DRAGON_BLOOD -> statUps.add(Pair(BuffStat.DRAGONBLOOD, ret.x))
                    DragonKnight.DRAGON_ROAR -> ret.hpR = -x / 100.0
                    Hero.STANCE, Paladin.STANCE, DarkKnight.STANCE -> statUps.add(Pair(BuffStat.STANCE, iProp.toInt()))
                    Magician.MAGIC_GUARD -> statUps.add(Pair(BuffStat.MAGIC_GUARD, x))
                    Cleric.INVINCIBLE -> statUps.add(Pair(BuffStat.INVINCIBLE, Integer.valueOf(x)))
                    Priest.HOLY_SYMBOL, GM.HOLY_SYMBOL, SuperGM.HOLY_SYMBOL -> statUps.add(Pair(BuffStat.HOLY_SYMBOL, x))
                    FPArchMage.INFINITY, ILArchMage.INFINITY, Bishop.INFINITY -> statUps.add(
                        Pair(BuffStat.INFINITY, x)
                    )
                    FPArchMage.MANA_REFLECTION, ILArchMage.MANA_REFLECTION, Bishop.MANA_REFLECTION -> statUps.add(
                        Pair(BuffStat.MANA_REFLECTION, 1)
                    )
                    Bishop.HOLY_SHIELD -> statUps.add(Pair(BuffStat.HOLY_SHIELD, Integer.valueOf(x)))
                    Priest.MYSTIC_DOOR, Hunter.SOUL_ARROW, Crossbowman.SOUL_ARROW -> statUps.add(
                        Pair(BuffStat.SOULARROW, x)
                    )
                    Ranger.PUPPET, Sniper.PUPPET, Outlaw.OCTOPUS, Corsair.WRATH_OF_THE_OCTOPI -> statUps.add(
                        Pair(BuffStat.PUPPET, 1)
                    )
                    Bowmaster.CONCENTRATE -> statUps.add(Pair(BuffStat.CONCENTRATE, x))
                    Bowmaster.HAMSTRING -> {
                        statUps.add(Pair(BuffStat.HAMSTRING, x))
                        monsterStatus[MonsterStatus.SPEED] = x
                    }
                    Marksman.BLIND -> {
                        statUps.add(Pair(BuffStat.BLIND, Integer.valueOf(x)))
                        monsterStatus[MonsterStatus.ACC] = x
                    }
                    Bowmaster.SHARP_EYES, Marksman.SHARP_EYES -> statUps.add(
                        Pair(BuffStat.SHARP_EYES, ret.x shl 8 or ret.y)
                    )
                    Rogue.DARK_SIGHT -> statUps.add(
                        Pair(BuffStat.DARKSIGHT, x)
                    )
                    Hermit.MESO_UP -> statUps.add(Pair(BuffStat.MESOUP, Integer.valueOf(x)))
                    Hermit.SHADOW_PARTNER -> statUps.add(
                        Pair(BuffStat.SHADOWPARTNER, x)
                    )
                    ChiefBandit.MESO_GUARD -> statUps.add(Pair(BuffStat.MESOGUARD, x))
                    ChiefBandit.PICKPOCKET -> statUps.add(Pair(BuffStat.PICKPOCKET, x))
                    NightLord.SHADOW_STARS -> statUps.add(Pair(BuffStat.SHADOW_CLAW, 0))
                    Pirate.DASH, Beginner.SPACE_DASH -> {
                        statUps.add(Pair(BuffStat.DASH2, ret.x))
                        statUps.add(Pair(BuffStat.DASH, ret.y))
                    }
                    Corsair.SPEED_INFUSION, Buccaneer.SPEED_INFUSION -> statUps.add(
                        Pair(BuffStat.SPEED_INFUSION, x)
                    )
                    Outlaw.HOMING_BEACON, Corsair.BULLSEYE -> statUps.add(
                        Pair(BuffStat.HOMING_BEACON, x)
                    )
                    Fighter.AXE_BOOSTER, Fighter.SWORD_BOOSTER, Page.BW_BOOSTER, Page.SWORD_BOOSTER, Spearman.POLEARM_BOOSTER, Spearman.SPEAR_BOOSTER, Hunter.BOW_BOOSTER, Crossbowman.CROSSBOW_BOOSTER, Assassin.CLAW_BOOSTER, Bandit.DAGGER_BOOSTER, FPMage.SPELL_BOOSTER, ILMage.SPELL_BOOSTER, Brawler.KNUCKLER_BOOSTER, Gunslinger.GUN_BOOSTER -> statUps.add(
                        Pair(BuffStat.BOOSTER, x)
                    )
                    Hero.M_WARRIOR, Paladin.M_WARRIOR, FPArchMage.M_WARRIOR, ILArchMage.M_WARRIOR, Bishop.M_WARRIOR, Bowmaster.M_WARRIOR, Marksman.M_WARRIOR, NightLord.M_WARRIOR, Shadower.M_WARRIOR, Corsair.M_WARRIOR, Buccaneer.M_WARRIOR -> statUps.add(
                        Pair(BuffStat.M_WARRIOR, x)
                    )
                    Ranger.SILVER_HAWK, Sniper.GOLDEN_EAGLE -> {
                        statUps.add(Pair(BuffStat.SUMMON, 1))
                        monsterStatus[MonsterStatus.STUN] = 1
                    }
                    FPArchMage.ELQUINES, Marksman.FROST_PREY -> {
                        statUps.add(Pair(BuffStat.SUMMON, 1))
                        monsterStatus[MonsterStatus.FREEZE] = 1
                    }
                    Priest.SUMMON_DRAGON, Bowmaster.PHOENIX, ILArchMage.IFRIT, Bishop.BAHAMUT, DarkKnight.BEHOLDER, Outlaw.GAVIOTA, DawnWarrior.SOUL -> statUps.add(
                        Pair(BuffStat.SUMMON, Integer.valueOf(1))
                    )
                    Rogue.DISORDER -> {
                        monsterStatus[MonsterStatus.WATK] = ret.x
                        monsterStatus[MonsterStatus.WDEF] = ret.y
                    }
                    Corsair.HYPNOTIZE -> monsterStatus[MonsterStatus.INERTMOB] = 1
                    NightLord.NINJA_AMBUSH, Shadower.NINJA_AMBUSH -> monsterStatus[MonsterStatus.NINJA_AMBUSH] =
                        ret.damage
                    Page.THREATEN -> {
                        monsterStatus[MonsterStatus.WATK] = ret.x
                        monsterStatus[MonsterStatus.WDEF] = ret.y
                    }
                    Crusader.AXE_COMA, Crusader.SWORD_COMA, Crusader.SHOUT, WhiteKnight.CHARGE_BLOW, Hunter.ARROW_BOMB, ChiefBandit.ASSAULTER, Shadower.BOOMERANG_STEP, Brawler.BACK_SPIN_BLOW, Brawler.DOUBLE_UPPERCUT, Buccaneer.DEMOLITION, Buccaneer.SNATCH, Buccaneer.BARRAGE, Gunslinger.BLANK_SHOT, DawnWarrior.COMA -> monsterStatus[MonsterStatus.STUN] =
                        1
                    NightLord.TAUNT, Shadower.TAUNT -> {
                        monsterStatus[MonsterStatus.SHOWDOWN] = ret.x
                        monsterStatus[MonsterStatus.MDEF] = ret.x
                        monsterStatus[MonsterStatus.WDEF] = ret.x
                    }
                    ILWizard.COLD_BEAM, ILMage.ICE_STRIKE, ILArchMage.BLIZZARD, ILMage.ELEMENT_COMPOSITION, Sniper.BLIZZARD, Outlaw.ICE_SPLITTER, FPArchMage.PARALYZE -> {
                        monsterStatus[MonsterStatus.FREEZE] = 1
                        ret.duration *= 2 // freezing skills are a little strange
                    }
                    FPWizard.SLOW, ILWizard.SLOW -> monsterStatus[MonsterStatus.SPEED] = ret.x
                    FPWizard.POISON_BREATH, FPMage.ELEMENT_COMPOSITION -> monsterStatus[MonsterStatus.POISON] = 1
                    Priest.DOOM -> monsterStatus[MonsterStatus.DOOM] = 1
                    ILMage.SEAL, FPMage.SEAL -> monsterStatus[MonsterStatus.SEAL] = 1
                    Hermit.SHADOW_WEB -> monsterStatus[MonsterStatus.SHADOW_WEB] = 1
                    FPArchMage.FIRE_DEMON, ILArchMage.ICE_DEMON -> {
                        monsterStatus[MonsterStatus.POISON] = 1
                        monsterStatus[MonsterStatus.FREEZE] = 1
                    }
                }
            }
            if (ret.isMorph()) {
                statUps.add(Pair(BuffStat.MORPH, ret.morphId))
            }
            if (ret.ghost > 0 && !skill) {
                statUps.add(Pair(BuffStat.GHOST_MORPH, ret.ghost))
            }
            ret.monsterStatus = monsterStatus
            ret.statUps = statUps
            return ret
        }

        fun loadSkillEffectFromData(source: Data, skillId: Int, overtime: Boolean) = loadFromData(source, skillId, skill = true, overtime)

        fun loadItemEffectFromData(source: Data?, itemId: Int) = loadFromData(source, itemId, skill = false, overTime = false)

        fun addBuffStatPairToListIfNotZero(list: List<Pair<BuffStat, Int>>, buffStat: BuffStat, value: Int) {
            if (value != 0) {
                list.toMutableList().add(Pair(buffStat, value))
            }
        }



    }
}