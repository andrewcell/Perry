package server.maps

import client.BuffStat
import client.Character
import client.Client
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.PetDataFactory
import client.status.MonsterStatus
import client.status.MonsterStatusEffect
import constants.ItemConstants
import kotlinx.coroutines.Job
import mu.KLogging
import net.server.Server
import scripting.map.MapScriptManager
import server.InventoryManipulator
import server.ItemInformationProvider
import server.Portal
import server.events.gm.Coconut
import server.events.gm.Fitness
import server.events.gm.OxQuiz
import server.events.gm.Snowball
import server.life.*
import tools.CoroutineManager
import tools.PacketCreator
import tools.packet.*
import java.awt.Point
import java.awt.Rectangle
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.round
import kotlin.random.Random

class GameMap(val mapId: Int, val world: Int, val channel: Int, val returnMapId: Int, monsterRateFloat: Float) {
    private val charactersLock = ReentrantReadWriteLock()
    val mapObjects = mutableMapOf<Int, MapObject>()
    private val monsterSpawn = mutableListOf<SpawnPoint>()
    val spawnedMonsterOnMap = AtomicInteger(0)
    val characters = mutableSetOf<Character>()
    val portals = mutableMapOf<Int, Portal>()
    private val areas = mutableListOf<Rectangle>()
    var footholds: FootholdTree? = null
    private val chrLock = ReentrantReadWriteLock(true)
    private val objectLock: ReentrantReadWriteLock = ReentrantReadWriteLock(true)
    private val chrRLock: ReentrantReadWriteLock.ReadLock = chrLock.readLock()
    private val objectRLock: ReentrantReadWriteLock.ReadLock = objectLock.readLock()
    private val chrWLock: ReentrantReadWriteLock.WriteLock = chrLock.writeLock()
    private val objectWLock = objectLock.writeLock()
    private var monsterRate = round(monsterRateFloat).toInt()
    private var runningOid = 100
    private var dropsOn = true
    var everLast = false
    private var mapEffect: MapEffect? = null
    var onFirstUserEnter = ""
    var onUserEnter = ""
    var fieldLimit = 0
    var mobInterval = 8000
    var docked = false
    var boat = false
    var timeLimit: Long? = null
    private var mapMonitor: Job? = null
    var forcedReturnMap = 999999999
    var clock = false
    var timeMob: Pair<Int?, String?>? = null
    var mapName = ""
    var streetName = ""
    var town = false
    var hpDec = 0
    var hpDecProtect = 0
    var mobCapacity = -1
    // Rice cake
    private var riceCakeNum = 0
    private var allowHPQSummon = false
    // Event
    var eventStarted = false
    private var snowball0: Snowball? = null
    private var snowball1: Snowball? = null
    var coconut: Coconut? = null
    var fieldType = -1
    //var team = 0
    var fitness: Fitness? = null
    //var snowballAttack: Long? = null
    var ox: OxQuiz? = null
    var isOxQuiz = false

    init {
        if (this.monsterRate == 0) {
            this.monsterRate = 1
        }
    }

    fun broadcastMessage(packet: ByteArray) = broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null)

    fun broadcastMessage(packet: ByteArray, rangedFrom: Point) = broadcastMessage(null, packet, 722500.0, rangedFrom)

    fun broadcastMessage(source: Character, packet: ByteArray) {
        chrRLock.lock()
        try {
            characters.forEach { if (it != source) it.client.announce(packet) }
        } finally {
            chrRLock.unlock()
        }
    }

    fun broadcastMessage(source: Character, packet: ByteArray, repeatToSource: Boolean) {
        broadcastMessage(if (repeatToSource) null else source, packet, Double.POSITIVE_INFINITY, source.position)
    }

    fun broadcastMessage(source: Character, packet: ByteArray, repeatToSource: Boolean, ranged: Boolean) {
        broadcastMessage(if (repeatToSource) null else source, packet, if (ranged) 722500.0 else Double.POSITIVE_INFINITY, source.position)
    }
    fun broadcastMessage(source: Character? = null, packet: ByteArray, rangedFrom: Point?) = broadcastMessage(source, packet, 722500.0, rangedFrom)

    fun broadcastMessage(source: Character?, packet: ByteArray, rangeSq: Double, rangedFrom: Point?) {
        chrRLock.lock()
        try {
            characters.forEach {
                if (it != source) {
                    if (rangeSq < Double.POSITIVE_INFINITY && rangedFrom != null) {
                        if (rangedFrom.distanceSq(it.position) <= rangeSq) {
                            it.client.announce(packet)
                        }
                    } else {
                        it.client.announce(packet)
                    }
                }
            }
        } finally {
            chrRLock.unlock()
        }
    }

    fun broadcastGMMessage(packet: ByteArray) = broadcastGMMessage(null, packet, Double.POSITIVE_INFINITY, null)

    fun broadcastGMMessage(source: Character, packet: ByteArray, repeatToSource: Boolean) {
        broadcastGMMessage(if (repeatToSource) null else source, packet, Double.POSITIVE_INFINITY, source.position)
    }

    private fun broadcastGMMessage(source: Character?, packet: ByteArray, rangeSq: Double, rangedFrom: Point?) {
        chrRLock.lock()
        try {
            characters.forEach {
                if (it != source && it.isGM()) {
                    if (rangeSq < Double.POSITIVE_INFINITY && rangedFrom != null) {
                        if (rangedFrom.distanceSq(it.position) <= rangeSq) it.client.announce(packet)
                    } else {
                        it.client.announce(packet)
                    }
                }
            }
        } finally {
            chrRLock.unlock()
        }
    }

    fun broadcastGMMessage(source: Character, packet: ByteArray) {
        chrRLock.lock()
        try {
            characters.forEach { if (it != source && (it.gmLevel) > source.gmLevel) it.client.announce(packet) }
        } finally {
            chrRLock.unlock()
        }
    }

    fun getMapObjectsInRect(box: Rectangle, types: List<MapObjectType>): List<MapObject> {
        objectRLock.lock()
        val ret = mutableListOf<MapObject>()
        try {
            mapObjects.values.forEach {
                if (types.contains(it.objectType) && box.contains(it.position))
                    ret.add(it)
            }
        } finally {
            objectRLock.unlock()
        }
        return ret
    }

    fun getReturnMap() = Server.getWorld(world).getChannel(channel).mapFactory.getMap(returnMapId)

    fun setReactorState() {
        objectRLock.lock()
        try {
            mapObjects.values.forEach { o ->
                if (o.objectType == MapObjectType.REACTOR) {
                    o as Reactor
                    if (o.state < 1) {
                        o.state = 1
                        broadcastMessage(GameplayPacket.triggerReactor(o, 1))
                    }
                }
            }
        } finally {
            objectRLock.unlock()
        }
    }

    fun addMapObject(mapObject: MapObject) {
        objectWLock.lock()
        try {
            incrementRunningOid(false)
            mapObject.objectId = runningOid
            mapObjects[runningOid] = mapObject
            incrementRunningOid()
        } finally {
            objectWLock.unlock()
        }
    }

    fun addMapTimer(time: Int) {
        timeLimit = System.currentTimeMillis() + (time * 1000)
        broadcastMessage(PacketCreator.getClock(time))
        mapMonitor = CoroutineManager.register({
            if ((timeLimit != 0.toLong()) && timeLimit!! < System.currentTimeMillis()) {
                warpEveryone(forcedReturnMap)
                if (characters.isEmpty()) {
                    resetReactors()
                    killAllMonsters()
                    clearDrops()
                    timeLimit = 0
                    if (mapId in 922240101..922240119) toggleHiddenNpc(9001108)
                    mapMonitor?.cancel(CancellationException())
                    mapMonitor = null
                }
            }
        }, 1000, 0)
    }

    fun addMonsterSpawn(monster: Monster, mobTime: Int, team: Int) {
        val newPos = calcPointBelow(monster.position) ?: return
        newPos.y -= 1
        val sp = SpawnPoint(monster, newPos, !monster.stats.isMobile(), mobTime, mobInterval, team)
        monsterSpawn.add(sp)
        if (sp.shouldSpawn() || mobTime == -1) sp.getMonster()?.let { spawnMonster(it, true) }
    }

    fun addArea(rec: Rectangle) = areas.add(rec)

    fun addPlayer(chr: Character) {
        chrWLock.lock()
        try {
            characters.add(chr)
        } finally {
            chrWLock.unlock()
        }
        chr.mapId = mapId
        if (onFirstUserEnter.isNotEmpty() && !chr.hasEntered(onFirstUserEnter, mapId) && MapScriptManager.scriptExists(onFirstUserEnter, true)) {
            if (getAllPlayer().size <= 1) {
                chr.enteredScript(onFirstUserEnter, mapId)
                MapScriptManager.getMapScript(chr.client, onFirstUserEnter, true)
            }
        }
        if (onUserEnter.isNotEmpty()) {
            // if (onUserEnter == "cygnusTest" && (mapId < 913040000 || mapId > 913040006)) chr.saveLocation("INTRO")
            MapScriptManager.getMapScript(chr.client, onUserEnter, false)
        }
        if (FieldLimit.CANNOTUSEMOUNTS.check(fieldLimit) && chr.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
            chr.cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING)
            chr.cancelBuffStats(BuffStat.MONSTER_RIDING)
        }
        if (mapId == 923010000 && getMonsterById(9300102) == null) { // Kenta's Mount quest
            LifeFactory.getMonster(9300102)?.let { spawnMonsterOnGroundBelow(it, Point(77, 426)) }
        } else if (mapId == 910110000) { // Henesys Party quest
            chr.client.announce(PacketCreator.getClock(15 * 60))
            CoroutineManager.register({
                if (mapId == 910110000)
                    chr.client.player?.changeMap(chr.client.getChannelServer().mapFactory.getMap(925020000))
            }, (15 * 60 * 1000 + 3000).toLong(), 0)
        }
        if (chr.pet != null) {
            val pos = chr.position
            pos.y -= 13
            chr.pet?.pos = pos
            chr.announce(CashPacket.showPet(chr, chr.pet, remove = false, hunger = true))
            chr.pet?.let { chr.startFullnessSchedule(PetDataFactory.getHunger(it.itemId), it) }
        }
        if (chr.petAutoHp > 0) {
            chr.announce(CashPacket.sendPetAutoHpPot(chr.petAutoHp))
        }
        /* if (chr.getPetAutoMP() > 0) {
         chr.announce(PacketCreator.sendAutoHpPot(chr.getPetAutoMP()))
        }*/
        if (chr.hidden) {
            broadcastGMMessage(chr, GameplayPacket.spawnPlayerMapObject(chr), false)
            chr.announce(PacketCreator.getGMEffect(0x10, 1))
        } else {
            broadcastMessage(chr, GameplayPacket.spawnPlayerMapObject(chr), false)
        }
        val allDiseases = chr.allDiseases
        if (allDiseases.isNotEmpty()) {
            allDiseases.values.forEach {
                broadcastMessage(chr, CharacterPacket.giveForeignDeBuff(chr.id, it.debuff, it.skill), false)
            }
        }
        sendObjectPlacement(chr.client)
        if (isStartingEventMap() && !eventStarted) chr.map.getPortal("join00")?.portalStatus = false
        if (hasForcedEquip()) chr.client.announce(GameplayPacket.showForcedEquip(-1))
        if (specialEquip()) chr.client.announce(MiniGamePacket.coconutScore(0, 0))
        objectWLock.lock()
        try {
            var key = chr.objectId
            while (mapObjects.containsKey(key)) {
                key++
            }
            chr.objectId = key
            mapObjects[key] = chr
        } finally {
            objectWLock.unlock()
        }
        chr.playerShop?.let { addMapObject(it) }
        val summonStat = chr.getStatForBuff(BuffStat.SUMMON)
        if (summonStat != null) {
            val summon = chr.summons[summonStat.sourceId] ?: return
            summon.position = chr.position
            chr.map.spawnSummon(summon)
            updateMapObjectVisibility(chr, summon)
        }
        mapEffect?.sendStartData(chr.client)
        chr.client.announce(PacketCreator.resetForcedStats())
        if (chr.eventInstance?.isTimerStarted() == true) {
            chr.eventInstance?.let { chr.client.announce(PacketCreator.getClock((it.getTimeLeft() / 1000).toInt())) }
        }
        if (chr.fitness?.isTimerStarted() == true) {
            chr.fitness?.let { chr.client.announce(PacketCreator.getClock((it.getTimeLeft() / 1000).toInt())) }
        }
        if (chr.ola?.isTimerStarted() == true) {
            chr.ola?.let { chr.client.announce(PacketCreator.getClock((it.getTimeLeft() / 1000).toInt())) }
        }
        if (mapId == 109060000) chr.announce(MiniGamePacket.rollSnowBall(true, 0, null, null))
        if (clock) {
            val cal = Calendar.getInstance()
            chr.client.announce(PacketCreator.getClockTime(cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE], cal[Calendar.SECOND]))
        }
        if (hasBoat() == 2) {
            chr.client.announce(PacketCreator.boatPacket(true))
        } else if (hasBoat() == 1 && (chr.mapId != 200090000 || chr.mapId != 200090010)) {
            chr.client.announce(PacketCreator.boatPacket(false))
        }
        chr.receivePartyMemberHp()
    }

    fun movePlayer(chr: Character, newPosition: Point) {
        chr.position = newPosition
        val visibleObjects = chr.visibleMapObjects
        val visibleObjectsNow = visibleObjects.toTypedArray()
        try {
            visibleObjectsNow.forEach {
                if (mapObjects[it.objectId] == it) {
                    updateMapObjectVisibility(chr, it)
                } else {
                    chr.removeVisibleMapObject(it)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error cause when handle move player" }
        }
        getMapObjectsInRange(chr.position, 722500.0, rangedMapObjectTypes).forEach {
            if (!chr.isMapObjectVisible(it)) {
                if (it.objectType != MapObjectType.MONSTER) {
                    it.sendSpawnData(chr.client)
                    chr.addVisibleMapObject(it)
                }
            }
        }
    }

    fun removePlayer(chr: Character) {
        chrWLock.lock()
        try { characters.remove(chr) } finally { chrWLock.unlock() }
        removeMapObject(chr)
        if (!chr.hidden) {
            broadcastMessage(GameplayPacket.removePlayerFromMap(chr.id))
        } else {
            broadcastGMMessage(GameplayPacket.removePlayerFromMap(chr.id))
        }
        chr.controlledMonsters.forEach {
            it.controller = null
            it.controllerHasAggro = false
            it.controllerKnowsAboutAggro = false
            updateMonsterController(it)
        }
        chr.leaveMap()
        chr.cancelMapTimeLimitTask()
        chr.summons.values.forEach {
            if (it.isStationary()) chr.cancelBuffStats(BuffStat.PUPPET) else removeMapObject(it)
        }
    }

    //private fun running

    private fun spawnAndAddRangedMapObject(mapObject: MapObject, packetBakery: DelayedPacketCreation, condition: SpawnCondition? = null) {
        chrRLock.lock()
        try {
            incrementRunningOid(false)
            mapObject.objectId = runningOid
            characters.forEach {
                if (condition == null || condition.canSpawn(it)) {
                    if (it.position.distanceSq(mapObject.position) <= 722500) {
                        packetBakery.sendPackets(it.client)
                        it.addVisibleMapObject(mapObject)
                    }
                }
            }
        } finally {
            chrRLock.unlock()
        }
        objectWLock.lock()
        try {
            incrementRunningOid(false)
            mapObject.objectId = runningOid
            mapObjects[runningOid] = mapObject
        } finally {
            objectWLock.unlock()
        }
        incrementRunningOid()
    }

    fun removeMapObject(obj: MapObject) {
        objectWLock.lock()
        try {
            mapObjects.remove(obj.objectId)
        } finally {
            objectWLock.unlock()
        }
    }

    private fun calcPointBelow(initial: Point): Point? {
        val fh = footholds?.findBelow(initial) ?: return null
        var dropY = fh.p1.y
        if (!fh.isWall() && fh.p1.y != fh.p2.y) {
            val s1 = abs(fh.p2.y - fh.p1.y).toDouble()
            val s2 = abs(fh.p2.x - fh.p1.x).toDouble()
            val s5 = cos(atan((s2 / s1))) * (abs(initial.x - fh.p1.x) / cos(atan((s1 / s2))))
            dropY = if (fh.p2.y < fh.p1.y) (fh.p1.y - s5).toInt() else (fh.p1.y + s5).toInt()
        }
        return Point(initial.x, dropY)
    }

    private fun calcDropPos(initial: Point, fallback: Point): Point = calcPointBelow(Point(initial.x, initial.y - 50)) ?: fallback

    private fun dropFromMonster(chr: Character, mob: Monster) {
        if (mob.dropsDisabled || !dropsOn) return
        val dropType = if (mob.stats.explosiveReward) {
            3
        } else if (mob.stats.ffaLoot) {
            2
        } else if (chr.party != null) {
            1
        } else {
            0
        }
        val mobPos = mob.position.x
        val chServerRate = chr.dropRate
        var d = 1
        val pos = Point(0, mob.position.y)
        val dropEntry = MonsterInformationProvider.retrieveDrop(mob.id).shuffled()
        dropEntry.forEach { e ->
            var max = 999999
            if (e.itemId != 0 && e.chance <= 50000) max = 2799999
            // if (e.itemId != 0 && ItemInformationProvider.getInventoryType(e.itemId) == InventoryType.EQUIP) max = 2099999
            val v0 = Random.nextInt(max)
            val v1 = e.chance * chServerRate
            if (v0 < v1) {
                pos.x = getXByDropType(dropType, mobPos, d)
                if (e.itemId == 0) {// Meso
                    var mesos = Random.nextInt(e.maximum - e.minimum + e.minimum)
                    if (mesos > 0) {
                        if (chr.getBuffedValue(BuffStat.MESOUP) != null) {
                            chr.getBuffedValue(BuffStat.MESOUP)?.let { mesos = (mesos * it.toDouble() / 100.0).toInt() }
                        }
                        spawnMesoDrop(mesos * chr.mesoRate, mob.position, pos, mob, chr, false, dropType.toByte())
                    }
                } else {
                    checkEquipAndSpawnDrop(chr, mob, dropType, pos, e)
                }
                d++
            }
        }
        val globalEntry = MonsterInformationProvider.globalDrops
        globalEntry.forEach { e ->
            if (e.itemId == 4030012 && mob.stats.level > 30) return@forEach
            if (Random.nextInt(999999) < e.chance) {
                pos.x = getXByDropType(dropType, mobPos, d)
                //if (e.itemId == 0) chr.cashShop.gainCash(1, 80)
                checkEquipAndSpawnDrop(chr, mob, dropType, pos, e)
                d++
            }
        }
    }

    fun allDropItem(chr: Character) {
        val items = chr.map.getMapObjectsInRange(chr.position, Double.MAX_VALUE, listOf(MapObjectType.ITEM))
        items.forEach {
            it as MapItem
            // logger.debug { "Dropping ${it.getItemId()}" }
            if (it.pickedUp) {
                chr.client.announce(PacketCreator.enableActions())
                return
            }
            if (it.meso > 0) {
                if (chr.party != null && it.ownerId != chr.id) {
                    val mesoAmm = it.meso
                    if (mesoAmm > 50000 * chr.mesoRate) {
                        return
                    }
                    if (!it.playerDrop) {
                        var partyNum = 0
                        chr.party?.members?.forEach { partyMember ->
                            if (partyMember.online && partyMember.mapId == chr.map.mapId && partyMember.channel == chr.client.channel) {
                                partyNum++
                            }
                        }
                        chr.party?.members?.forEach { partyMember ->
                            if (partyMember.online && partyMember.mapId == chr.map.mapId) {
                                val someCharacter = partyMember.id?.let { it1 -> chr.client.getChannelServer().players.getCharacterById(it1) }
                                if (someCharacter != null) {
                                    someCharacter.gainMeso(mesoAmm / partyNum,
                                        show = true,
                                        enableActions = false,
                                        inChat = false
                                    )
                                    chr.client.announce(PacketCreator.enableActions())
                                }
                            }
                        }
                    } else {
                        chr.gainMeso(it.meso, show = true, enableActions = true, inChat = false)
                    }
                } else {
                    chr.gainMeso(it.meso, show = true, enableActions = true, inChat = false)
                }
                it.pickedUp = true
                chr.map.broadcastMessage(GameplayPacket.removeItemFromMap(it.objectId, 0, chr.id), it.position)
                chr.map.removeMapObject(it)
            } else {
                if (it.item == null) return
                if (InventoryManipulator.addFromDrop(chr.client, it.item, true)) {
                    it.pickedUp = true
                    chr.map.broadcastMessage(GameplayPacket.removeItemFromMap(it.objectId, 0, chr.id), it.position)
                    chr.map.removeMapObject(it)
                    chr.client.announce(CharacterPacket.getInventoryFull())
                    chr.client.announce(PacketCreator.enableActions())
                }
            }
        }
    }

    fun clearDrops() {
        getMapObjectsInRange(Point(0, 0), Double.POSITIVE_INFINITY, listOf(MapObjectType.ITEM)).forEach {
            removeMapObject(it)
        }
    }

    fun clearDrops(chr: Character) {
        val items = chr.map.getMapObjectsInRange(chr.position, Double.POSITIVE_INFINITY, listOf(MapObjectType.ITEM))
        items.forEach {
            chr.map.removeMapObject(it)
            chr.map.broadcastMessage(GameplayPacket.removeItemFromMap(it.objectId, 0, chr.id))
        }
    }

    fun addPortal(p: Portal) = portals.put(p.id, p)

    fun getPortal(portalId: Int) = portals[portalId]

    fun getPortal(portalName: String) = portals.values.find { it.name == portalName }

    fun findClosestPortal(from: Point): Portal? {
        var closest: Portal? = null
        portals.values.forEach {
            val distance = it.position?.distanceSq(from) ?: return null
            val shortestDistance = Double.POSITIVE_INFINITY
            if (distance < shortestDistance) {
                closest = it
            }
        }
        return closest
    }

    fun findClosestSpawnPoint(from: Point): Portal? {
        var shortestDistance = Double.POSITIVE_INFINITY
        var closest: Portal? = null
        portals.values.forEach {
            val distance = it.position?.distanceSq(from) ?: return null
            if (it.type in 0..2 && distance < shortestDistance && it.targetMapId == 999999999) {
                closest = it
                shortestDistance = distance
            }
        }
        return closest
    }

    fun getRandomSpawnPoint(): Portal {
        val spawnPoints = mutableListOf<Portal>()
        portals.values.forEach { if (it.type in 0..2) spawnPoints.add(it) }
        return spawnPoints[Random.nextInt(spawnPoints.size)]
    }

    fun spawnDoor(door: Door) {
        spawnAndAddRangedMapObject(door, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(GameplayPacket.spawnDoor(door.owner.id, door.targetPosition, false))
                door.owner.party?.let {
                    c.player?.let { player ->
                        if (door.owner.party != null && (door.owner == c.player || it.containsMembers(player.mpc))) {
                            c.announce(
                                InteractPacket.partyPortal(
                                    door.town.mapId,
                                    door.target.mapId,
                                    door.targetPosition
                                )
                            )
                        }
                    }
                }
                c.announce(GameplayPacket.spawnPortal(door.town.mapId, door.target.mapId, door.targetPosition))
                c.announce(PacketCreator.enableActions())
            }
        }, object : SpawnCondition {
            override fun canSpawn(chr: Character) = chr.mapId == door.target.mapId || chr == door.owner && chr.party == null
        })
    }

    private fun spawnDrop(drop1: Item, dropFrom: Point, position: Point, mob: Monster, chr: Character, dropType: Byte, questId: Int) {
        val dropTo = calcDropPos(position, mob.position)
        val mDrop = MapItem(0, drop1, dropTo, mob, chr, dropType, false, questId)
        spawnAndAddRangedMapObject(mDrop, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.player?.let { player ->
                    if (questId <= 0 || (player.getQuestStatus(questId) == 1 && player.needQuestItem(questId, drop1.itemId))
                    ) {
                        c.announce(ItemPacket.dropItemFromMapObject(mDrop, dropFrom, dropTo, 1))
                    }
                }
            }
        }, null)
        CoroutineManager.schedule(ExpireMapItemJob(mDrop), 180000)
    }

    private fun spawnMesoDrop(meso: Int, dropFrom: Point, position: Point, mob: Monster, owner: Character, playerDrop: Boolean, dropType: Byte) {
        val dropTo = calcDropPos(position, mob.position)
        val mDrop = MapItem(meso, null, dropTo, mob, owner, dropType, playerDrop, -1)
        spawnAndAddRangedMapObject(mDrop, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(ItemPacket.dropItemFromMapObject(mDrop, dropFrom, dropTo, 1))
            }
        }, null)
        CoroutineManager.schedule(ExpireMapItemJob(mDrop), 180000)
    }

    fun spawnMesoDrop(meso: Int, position: Point, dropper: MapObject, owner: Character?, playerDrop: Boolean, dropType: Byte) {
        val dropPos = calcDropPos(position, dropper.position)
        val mDrop = MapItem(meso, null, dropPos, dropper, owner, dropType, playerDrop, -1)
        spawnAndAddRangedMapObject(mDrop, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(ItemPacket.dropItemFromMapObject(mDrop, dropper.position, dropPos, 1))
            }
        }, null)
        CoroutineManager.schedule(ExpireMapItemJob(mDrop), 180000)
    }

    fun disappearingItemDrop(dropper: MapObject, owner: Character, item: Item, pos: Point) {
        val dropPos = calcDropPos(pos, pos)
        val drop = MapItem(0, item, dropPos, dropper, owner, 1, false, -1)
        broadcastMessage(ItemPacket.dropItemFromMapObject(drop, dropper.position, dropPos, 3), drop.position)
    }

    private fun activateItemReactors(drop: MapItem, c: Client) {
        val item = drop.item ?: return
        run foreach@ {
            getAllReactor().forEach {
                it as Reactor
                if (it.getReactorType() == 100) {
                    if (it.getReactItem(0)?.first == item.itemId && it.getReactItem(0)?.second == item.quantity.toInt()) {
                        if (it.getArea().contains(drop.position)) {
                            if (!it.timerActive) {
                                CoroutineManager.schedule(ActivateItemReactor(drop, it, c), 5000)
                                it.timerActive = true
                                return@foreach
                            }
                        }
                    }
                }
            }
        }
    }

    fun spawnItemDrop(dropper: MapObject, owner: Character?, item: Item, pos: Point, ffaDrop: Boolean, playerDrop: Boolean) {
        val dropPos = calcDropPos(pos, pos)
        val drop = MapItem(0, item, dropPos, dropper, owner, if (ffaDrop) 2 else 0, playerDrop, -1)
        spawnAndAddRangedMapObject(drop, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(ItemPacket.dropItemFromMapObject(drop, dropper.position, dropPos, 1))
            }
        })
        broadcastMessage(ItemPacket.dropItemFromMapObject(drop, dropper.position, dropPos, 0))
        if (!everLast) {
            CoroutineManager.schedule(ExpireMapItemJob(drop), 180000)
            owner?.let { activateItemReactors(drop, it.client) }
        }

    }

    fun spawnQuestItemDrop(dropper: MapObject, owner: Character, item: Item, pos: Point, ffaDrop: Boolean, playerDrop: Boolean, questId: Int) {
        val dropPos = calcDropPos(pos, pos)
        val drop = MapItem(0, item, dropPos, dropper, owner, if (ffaDrop) 2 else 0, playerDrop, questId)
        spawnAndAddRangedMapObject(drop, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.player?.let { player ->
                    if (questId <= 0 || (player.getQuestStatus(questId) == 1 && player.needQuestItem(questId, item.itemId))) {
                        c.announce(ItemPacket.dropItemFromMapObject(drop, dropper.position, dropPos, 1))
                    }
                }
            }
        }, null)
        if (!everLast) {
            CoroutineManager.schedule(ExpireMapItemJob(drop), 180000)
            activateItemReactors(drop, owner.client)
        }
    }

    fun spawnMist(mist: Mist, duration: Int, poison: Boolean, fake: Boolean) {
        addMapObject(mist)
        broadcastMessage(if (fake) mist.makeFakeSpawnData(30) else mist.makeSpawnData())
        val positionSchedule = if (poison) {
            CoroutineManager.register({
                val affectedMonsters = getMapObjectsInBox(mist.mistPosition, listOf(MapObjectType.MONSTER))
                affectedMonsters.forEach {
                    if (mist.makeChanceResult()) {
                        val poisonEffect = MonsterStatusEffect(mutableMapOf(Pair(MonsterStatus.POISON, 1)), mist.getSourceSkill(), null, false)
                        mist.owner?.let { it1 -> (it as Monster).applyStatus(it1, poisonEffect, true, duration.toLong()) }
                    }
                }
            }, 2000, 2500)
        } else {
            null
        }
        CoroutineManager.schedule({
            removeMapObject(mist)
            positionSchedule?.cancel()
            broadcastMessage(mist.makeDestroyData())
        }, duration.toLong())
    }

    fun spawnMonster(monster: Monster, newSpawn: Boolean) {
        monster.map = this
        spawnAndAddRangedMapObject(monster, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(GameplayPacket.spawnMonster(monster, newSpawn))
            }
        })
        updateMonsterController(monster)
        if (monster.stats.dropPeriod > 0) {
            when (monster.id) {
                9300102 -> monsterItemDrop(monster, Item(4031507, 0, 1), monster.stats.dropPeriod.toLong())
                9300061 -> monsterItemDrop(monster, Item(4001101, 0, 1), (monster.stats.dropPeriod / 3).toLong())
                else -> logger.warn { "Unknown timed mob detected: ${monster.id}" }
            }
        }
        spawnedMonsterOnMap.incrementAndGet()
        val selfDestruction = monster.stats.selfDestruction
        if (monster.stats.removeAfter > 0 || selfDestruction != null && selfDestruction.hp < 0) {
            CoroutineManager.schedule({
                if (selfDestruction == null)
                    killMonster(monster, getAllPlayer()[0] as Character, false)
                else
                    killMonster(monster, getAllPlayer()[0] as Character,
                        withDrops = false,
                        secondTime = false,
                        animation = selfDestruction.action.toInt()
                    )
            }, (selfDestruction?.removeAfter?.times(1000) ?: monster.stats.removeAfter).toLong())
        }
        if (mapId == 910110000 && !allowHPQSummon) broadcastMessage(GameplayPacket.makeMonsterInvisible(monster)) // HPQ make monsters invisible
    }

    fun spawnMonsterWithEffect(monster: Monster, effect: Int, pos: Point) {
        val sPos = calcPointBelow(Point(pos.x, pos.y - 1)) ?: return
        sPos.y--
        monster.position = sPos
        if (mapId < 925020000 || mapId > 925030000) monster.dropsDisabled = true
        spawnAndAddRangedMapObject(monster, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(GameplayPacket.spawnMonster(monster, true, effect))
            }
        })
        if (monster.hasBossHpBar()) broadcastMessage(monster.makeBossHpBarPacket(), monster.position)
        updateMonsterController(monster)
        spawnedMonsterOnMap.incrementAndGet()
    }

    private fun spawnFakeMonster(monster: Monster) {
        monster.map = this
        monster.fake = true
        spawnAndAddRangedMapObject(monster, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(GameplayPacket.spawnFakeMonster(monster, 0))
            }
        })
        spawnedMonsterOnMap.incrementAndGet()
    }

    private fun makeMonsterReal(monster: Monster) {
        monster.fake = false
        broadcastMessage(GameplayPacket.makeMonsterReal(monster))
        updateMonsterController(monster)
    }

    fun spawnMonsterOnGroundBelow(mob: Monster, pos: Point) {
        var sPos = Point(pos.x, pos.y - 1)
        sPos = calcPointBelow(sPos) ?: return
        sPos.y--
        mob.position = sPos
        spawnMonster(mob, true)
    }

    fun spawnFakeMonsterOnGroundBelow(mob: Monster, pos: Point) {
        val sPos = getGroundBelow(pos)
        mob.position = sPos
        spawnFakeMonster(mob)
    }

    fun spawnReactor(reactor: Reactor) {
        reactor.map = this
        spawnAndAddRangedMapObject(
            reactor,
            object : DelayedPacketCreation {
                override fun sendPackets(c: Client) {
                    c.announce(reactor.makeSpawnData())
                }
            })
    }

    private fun respawnReactor(reactor: Reactor) {
        reactor.state = 0
        reactor.alive = true
        spawnReactor(reactor)
    }

    fun spawnRevives(monster: Monster) {
        monster.map = this
        spawnAndAddRangedMapObject(monster, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(GameplayPacket.spawnMonster(monster, false))
            }
        })
        updateMonsterController(monster)
        spawnedMonsterOnMap.incrementAndGet()
    }

    fun setSnowball(team: Int, ball: Snowball?) {
        when (team) {
            0 -> snowball0 = ball
            1 -> snowball1 = ball
            else -> return
        }
    }

    fun getSnowball(team: Int) = if (team == 0) snowball0 else snowball1

    fun spawnSummon(summon: Summon) {
        spawnAndAddRangedMapObject(summon, object : DelayedPacketCreation {
            override fun sendPackets(c: Client) {
                c.announce(GameplayPacket.spawnSummon(summon, true))
            }
        })
    }

    fun startMapEffect(message: String, itemId: Int, time: Long = 30000) {
        if (mapEffect != null) return
        mapEffect = MapEffect(message, itemId)
        mapEffect?.let {
            broadcastMessage(it.makeStartData())
            CoroutineManager.schedule({
                broadcastMessage(it.makeDestroyData())
                mapEffect = null
            }, time)
        }
    }

    private fun monsterItemDrop(m: Monster, item: Item, delay: Long) {
        val monsterItemDrop1 = CoroutineManager.register({
            if (getMonsterById(m.id) != null) {
                if (item.itemId == 4001101) {
                    broadcastMessage(InteractPacket.serverNotice(6, "The Moon Bunny made rice cake number ${riceCakeNum + 1}"))
                }
                spawnItemDrop(m, null, item, m.position, ffaDrop = true, playerDrop = true)
            }
        }, delay, delay)
        if (getMonsterById(m.id) == null) monsterItemDrop1.cancel(CancellationException())
    }

    private fun getMonsterById(id: Int): Monster? {
        objectRLock.lock()
        try {
            return mapObjects.values.find {
                it.objectType == MapObjectType.MONSTER && (it as Monster).id == id
            } as? Monster
        } finally {
            objectRLock.unlock()
        }
    }

    fun getMonsterByOid(oid: Int): Monster? {
        val mmo = mapObjects[oid]
            ?: return mapObjects.values.find {
                it.objectType == MapObjectType.MONSTER && it.objectId == oid
            } as Monster?
        return if (mmo.objectType == MapObjectType.MONSTER) mmo as Monster else null
    }

    fun countMonster(id: Int): Int {
        return getMapObjectsInRange(Point(0, 0), Double.POSITIVE_INFINITY, listOf(MapObjectType.MONSTER)).count {
            it as Monster
            it.id == id
        }
    }

    fun allMonster(): List<Monster> {
        return getMapObjectsInRange(Point(0, 0), Double.POSITIVE_INFINITY, listOf(MapObjectType.MONSTER)).filterIsInstance<Monster>()
    }

    fun damageMonster(chr: Character, monster: Monster, damage: Int): Boolean {
        if (monster.id == 8800000) {
            chr.map.mapObjects.values.forEach { o ->
                val mons = chr.map.getMonsterByOid(o.objectId)
                if (mons != null && mons.id in 8800003..8800010) return true
            }
        }
        if (monster.isAlive()) {
            var killed = false
            monster.monsterLock.lock()
            try {
                if (!monster.isAlive()) return false
                val cool = monster.stats.cool
                if (damage > 0) {
                    monster.damage(chr, damage, true)
                    if (!monster.isAlive()) killed = true
                } else if (monster.id in 8810002..8810009) {
                    chr.map.mapObjects.values.forEach { o ->
                        val mons = chr.map.getMonsterByOid(o.objectId)
                        if (mons?.id == 8810018) damageMonster(chr, mons, damage)
                    }
                }
            } finally {
                monster.monsterLock.unlock()
            }
            val selfDestruction = monster.stats.selfDestruction
            if (selfDestruction != null && selfDestruction.hp > -1){
                if (monster.hp <= selfDestruction.hp) {
                    killMonster(monster, chr,
                        withDrops = true,
                        secondTime = false,
                        animation = selfDestruction.action.toInt()
                    )
                    return true
                }
            }
            if (killed) {
                killMonster(monster, chr, true)
            }
            return true
        }
        return false
    }

    fun killMonster(id: Int) {
        mapObjects.values.forEach {
            if (it.objectType == MapObjectType.MONSTER) {
                it as Monster
                if (it.id == id) {
                    killMonster(it, getAllPlayer()[0] as Character, false)
                }
            }
        }
    }

    fun killMonster(monster: Monster, chr: Character?, withDrops: Boolean, secondTime: Boolean = false, animation: Int = 1) {
        if (monster.id == 8810018 && !secondTime) {
            CoroutineManager.schedule({
                killMonster(monster, chr, withDrops, true, 1)
                killAllMonsters()
            }, 3000)
            return
        }
        if (chr == null) {
            spawnedMonsterOnMap.decrementAndGet()
            monster.hp = 0
            broadcastMessage(GameplayPacket.killMonster(monster.objectId, animation), monster.position)
            removeMapObject(monster)
            return
        }
        val buff = monster.stats.buffToGive
        if (buff > -1) {
            getAllPlayer().forEach { c ->
                c as Character
                if (c.isAlive()) {
                    val statEffect = ItemInformationProvider.getItemEffect(buff)
                    c.client.announce(CharacterPacket.showOwnBuffEffect(buff, 1))
                    broadcastMessage(c, PacketCreator.showBuffEffect(c.id, buff, 1), false)
                    statEffect?.applyTo(c)
                }
            }
        }
        fun killedBoss() {
            if (monster.id == 8800002 || monster.id == 8810018) {
                Server.getWorld(world).channels.forEach { channel ->
                    channel.players.getAllCharacters().forEach { p ->
                        if (monster.id == 8810018 && p.mapId == 240000000) {
                            p.message("Mysterious power arose as I heard the powerful cry of the Nine Spirit Baby Dragon.")
                        }
                        val message = if (monster.id == 8810018) {
                            "수많은 도전 끝에 혼테일을 격파한 원정대여! 그대들이 진정한 리프레의 영웅이다!"
                        } else {
                            "수많은 도전 끝에 자쿰을 격파한 원정대여! 그대들이 진정한 엘나스의 영웅이다!"
                        }
                        broadcastMessage(InteractPacket.serverNotice(6, message))
                    }
                }
            }
        }
        killedBoss()
        spawnedMonsterOnMap.decrementAndGet()
        monster.hp = 0
        broadcastMessage(GameplayPacket.killMonster(monster.objectId, animation), monster.position)
        if (monster.stats.selfDestruction == null) {
            removeMapObject(monster)
        }
        if (monster.id in 8800003..8800010) {
            var makeZakReal = true
            run zak@ {
                mapObjects.values.forEach { obj ->
                    val mons = getMonsterById(obj.objectId)
                    if (mons != null) {
                        if (mons.id in 8800003..8800010) {
                            makeZakReal = false
                            return@zak
                        }
                    }
                }
            }
            if (makeZakReal) {
                run zak2@{
                    mapObjects.values.forEach { obj ->
                        val mons = chr.map.getMonsterByOid(obj.objectId)
                        if (mons?.id == 8800000) {
                            makeMonsterReal(mons)
                            updateMonsterController(mons)
                            return@zak2
                        }
                    }
                }
            }
        }
        val dropOwner = monster.killBy(chr) ?: chr
        if (withDrops && !monster.dropsDisabled) {
            dropFromMonster(dropOwner, monster)
        }
    }

    fun moveMonster(monster: Monster, reportedPos: Point) {
        monster.position = reportedPos
        chrRLock.lock()
        try {
            characters.forEach { updateMapObjectVisibility(it, monster) }
        } finally {
            chrRLock.unlock()
        }
    }

    fun killAllMonsters() {
        getMapObjectsInRange(Point(0, 0), Double.POSITIVE_INFINITY, listOf(MapObjectType.MONSTER)).forEach { o ->
            o as Monster
            spawnedMonsterOnMap.decrementAndGet()
            o.hp = 0
            broadcastMessage(GameplayPacket.killMonster(o.objectId, true), o.position)
            removeMapObject(o)
        }
    }

    fun destroyReactor(oid: Int) {
        val reactor = getReactorByOid(oid) ?: return
        broadcastMessage(GameplayPacket.destroyReactor(reactor))
        reactor.alive = false
        removeMapObject(reactor)
        reactor.timerActive = false
        if (reactor.delay > 0) {
            CoroutineManager.schedule({
                respawnReactor(reactor)
            }, reactor.delay.toLong())
        }
    }

    fun resetReactors() {
        objectRLock.lock()
        try {
            mapObjects.values.forEach { o ->
                if (o.objectType == MapObjectType.REACTOR) {
                    o as Reactor
                    o.state = 0
                    o.timerActive = false
                    broadcastMessage(GameplayPacket.triggerReactor(o, 0))
                }
            }
        } finally {
            objectRLock.unlock()
        }
    }

    fun shuffleReactors() {
        objectRLock.lock()
        val points = mutableListOf<Point>()
        try {
            mapObjects.values.forEach {
                if (it.objectType == MapObjectType.REACTOR) points.add(it.position)
            }
            points.shuffle()
            mapObjects.values.forEach { o ->
                if (o.objectType == MapObjectType.REACTOR)
                    o.position = points.removeAt(points.size - 1)
            }
        } finally {
            objectRLock.unlock()
        }
    }

    fun getPlayersInRange(box: Rectangle, chr: List<Character>): List<Character> {
        chrRLock.lock()
        return try {
            characters.filter { chr.contains(it.client.player) && box.contains(it.position) }
        } finally {
            chrRLock.unlock()
        }
    }

    fun getReactorById(id: Int): Reactor? {
        objectRLock.lock()
        try {
            return mapObjects.values.find { it.objectType == MapObjectType.REACTOR && (it as Reactor).rid == id } as? Reactor
        } finally {
            objectRLock.unlock()
        }
    }

    fun getReactorByOid(oid: Int): Reactor? {
        val mmo = mapObjects[oid]
            ?: return mapObjects.values.find {
                it.objectType == MapObjectType.REACTOR && it.objectId == oid
            } as? Reactor
        return if (mmo.objectType == MapObjectType.REACTOR) mmo as Reactor else null
    }

    fun getReactorByName(name: String): Reactor? {
        objectRLock.lock()
        try {
            return mapObjects.values.find { it.objectType == MapObjectType.REACTOR && (it as Reactor).name == name } as? Reactor
        } finally {
            objectRLock.unlock()
        }
    }

    private fun updateMonsterController(monster: Monster) {
        monster.monsterLock.lock()
        try {
            if (!monster.isAlive()) return
            if (monster.controller != null) {
               if (monster.controller?.get()?.map != this) {
                   monster.controller?.get()?.stopControllingMonster(monster)
               } else return
            }
            var minControlled = -1
            chrRLock.lock()
            var newController: Character? = null
            try {
               characters.forEach { c ->
                   if (!c.hidden && (c.controlledMonsters.size < minControlled || minControlled == -1)) {
                       minControlled = c.controlledMonsters.size
                       newController = c
                   }
               }
            } finally {
                chrRLock.unlock()
            }
            if (newController != null) {
                if (monster.stats.firstAttack) {
                    newController?.controlMonster(monster, true)
                    monster.controllerHasAggro = true
                    monster.controllerKnowsAboutAggro = true
                } else {
                    newController?.controlMonster(monster, false)
                }
            }
        } finally {
            monster.monsterLock.unlock()
        }
    }

    private fun updateMapObjectVisibility(chr: Character, mo: MapObject) {
        if (!chr.isMapObjectVisible(mo)) { //Monster entered view range
            if (mo.objectType == MapObjectType.SUMMON || mo.position.distanceSq(chr.position) <= 722500) {
                chr.addVisibleMapObject(mo)
                mo.sendSpawnData(chr.client)
            }
        } else if (mo.objectType != MapObjectType.SUMMON && mo.position.distanceSq(chr.position) > 722500) {
            chr.removeVisibleMapObject(mo)
            mo.sendDestroyData(chr.client)
        }
    }

    fun containsNpc(npcId: Int): Boolean {
        if (npcId == 9000066) return true
        objectRLock.lock()
        try {
            mapObjects.values.find { it.objectType == MapObjectType.NPC && (it as Npc).id == npcId } ?: return false
            return true
        } finally {
            objectRLock.unlock()
        }
    }

    fun getCharacterById(id: Int): Character? {
        chrRLock.lock()
        try {
            return characters.find { it.id == id }
        } finally {
            chrRLock.unlock()
        }
    }

    fun getCharacterByName(name: String): Character? {
        chrRLock.lock()
        try { return characters.find { it.name.equals(name, true) } }
        finally {
            chrRLock.unlock()
        }
    }

    fun getCharactersThreadSafe(): List<Character> {
        chrRLock.lock()
        try {
            return characters.toList()
        } finally {
            chrRLock.unlock()
        }
    }

    private fun getGroundBelow(pos: Point): Point {
        var sPos = Point(pos.x, pos.y - 1)
        sPos = calcPointBelow(sPos) ?: return Point(pos.x, pos.y - 1)
        sPos.y--
        return sPos
    }

    private fun getAllPlayer() = getMapObjectsInRange(Point(0, 0), Double.POSITIVE_INFINITY, listOf(MapObjectType.PLAYER))

    private fun getAllReactor() = getMapObjectsInRange(Point(0, 0), Double.POSITIVE_INFINITY, listOf(MapObjectType.REACTOR))

    fun getMapObjectsInRange(from: Point, rangeSq: Double, types: List<MapObjectType>): List<MapObject> {
        objectRLock.lock()
        try {
            return mapObjects.values.filter {
                if (types.contains(it.objectType)) {
                    from.distanceSq(it.position) <= rangeSq
                } else false
            }
        } finally {
            objectRLock.unlock()
        }
    }

    fun getMapObjectsInBox(box: Rectangle, types: List<MapObjectType>): List<MapObject> {
        val ret = mutableListOf<MapObject>()
        objectRLock.lock()
        try {
            mapObjects.values.forEach {
                if (types.contains(it.objectType)) {
                    if (box.contains(it.position)) {
                        ret.add(it)
                    }
                }
            }
            return ret
        } finally {
            objectRLock.unlock()
        }
    }

    private fun getXByDropType(dropType: Int, mobPos: Int, d: Int): Int {
        return mobPos + if (dropType == 3) {
            if (d % 2 == 0) 40 * (d + 1) / 2 else -(40 * (d / 2))
        } else {
            if (d % 2 == 0) 25 * (d + 1) / 2 else -(25 * (d / 2))
        }
    }

    private fun checkEquipAndSpawnDrop(chr: Character, mob: Monster, dropType: Int, pos: Point, e: MonsterDropEntry) {
        val iDrop = if (ItemConstants.getInventoryType(e.itemId) == InventoryType.EQUIP) {
            ItemInformationProvider.randomizeStats(ItemInformationProvider.getEquipById(e.itemId) as Equip)
        } else {
            Item(e.itemId, 0, (if (e.maximum != 1) Random.nextInt(e.maximum - e.minimum) + e.minimum else 1).toShort())
        }
        spawnDrop(iDrop, mob.position, pos, mob, chr, dropType.toByte(), e.questId.toInt())
    }

    private fun incrementRunningOid(lock: Boolean = true) {
        if (lock) objectRLock.lock()
        try {
            while (mapObjects.containsKey(runningOid)) {
                if (runningOid >= 30000) runningOid = 1000 //Lol, like there are monsters with the same oid NO
                runningOid++
            }
        } finally {
            if (lock) objectRLock.unlock()
        }
    }

    fun isEventMap() = mapId >= 109010000 && this.mapId < 109050000 || this.mapId in 109050002..109090000

    private fun isStartingEventMap() = when (mapId) {
        109040000, 109020001, 109010000, 109030001, 109030101 -> true
        else -> false
    }

    fun toggleDrops() {
        this.dropsOn = !dropsOn
    }

    fun toggleHiddenNpc(id: Int) {
        mapObjects.values.forEach {
            if (it.objectType == MapObjectType.NPC) {
                it as Npc
                if (it.id == id) {
                    it.hidden = !it.hidden
                    if (!it.hidden) // Should only be hidden upon changing maps
                        broadcastMessage(NpcPacket.spawnNpc(it))
                }
            }
        }
    }

    fun fired() {
        killAllMonsters()
        broadcastMessage(InteractPacket.serverNotice(6, "[안내] 맵 안에 문제가 발생하여 몬스터가 재소환 됩니다."))
    }

    fun respawn() {
        if (characters.isEmpty()) return
        val numShouldRespawn = (monsterSpawn.size - spawnedMonsterOnMap.get()) * monsterRate
        if (numShouldRespawn > 0) {
            val randomSpawn = monsterSpawn.toList().shuffled()
            var spawned = 0
            run fe@ {
                randomSpawn.forEach {
                    if (it.shouldSpawn()) {
                        it.getMonster()?.let { it1 -> spawnMonster(it1, true) }
                        spawned++
                    }
                    if (spawned >= numShouldRespawn) return@fe
                }
            }
        }
    }

    fun startEvent(chr: Character) {
        when (mapId) {
            109080000 -> {
                coconut = Coconut(this)
                coconut?.startEvent()
            }
            109040000 -> {
                fitness = Fitness(chr)
                fitness?.startFitness()
            }
            109020001 -> {
                if (ox == null) {
                    ox = OxQuiz(this)
                    ox?.sendQuestion()
                    isOxQuiz = true
                }
            }
            109060000 -> {
                if (getSnowball(chr.eventTeam) == null) {
                    setSnowball(0, Snowball(0, this))
                    setSnowball(1, Snowball(1, this))
                    getSnowball(chr.eventTeam)?.startEvent()
                }
            }
        }
    }

    fun getCharactersSize(): Int {
        var ret = 0
        charactersLock.readLock().lock()
        try {
            characters.forEach {
                if (!it.clone) ret++
            }
            return ret
        } finally {
            charactersLock.readLock().unlock()
        }
    }

    fun getEventNpc(): String? {
        return "Talk to ${when (mapId) {
            60000 -> "Paul!"
            104000000 -> "Jean!"
            200000000 -> "Martin!"
            220000000 -> "Tony!"
            else -> return null
        }}"
    }

    fun getForcedReturnMapAsMap() = Server.getWorld(world).getChannel(channel).mapFactory.getMap(forcedReturnMap)

    fun getCurrentPartyId(): Int {
        characters.forEach {
            if (it.getPartyId() != -1) return it.getPartyId()
        }
        return -1
    }

    private fun warpEveryone(to: Int) {
        characters.forEach { it.changeMap(to) }
    }

    fun warpOutByTeam(team: Int, mapId: Int) {
        characters.forEach {
            if (it.eventTeam == team) it.changeMap(mapId)
        }
    }

    fun hasEventNpc() = when (mapId) {
        60000, 104000000, 200000000, 220000000 -> true
        else -> false
    }

    private fun specialEquip() = fieldType == 4 || fieldType == 19

    private fun hasBoat() = if (docked) 2 else if (boat) 1 else 0

    private fun hasForcedEquip() = fieldType == 81 || fieldType == 82

    private fun isNonRangedType(type: MapObjectType) = when (type) {
        MapObjectType.NPC, MapObjectType.PLAYER, MapObjectType.HIRED_MERCHANT, MapObjectType.PLAYER_NPC, MapObjectType.MIST -> true
        else -> false
    }

    private fun sendObjectPlacement(c: Client) {
        val chr = c.player
        objectRLock.lock()
        try {
            mapObjects.values.forEachIndexed { k, it ->
                if (it.objectType == MapObjectType.SUMMON) {
                    it as Summon
                    if (it.owner == chr) {
                        if (chr.summons.isEmpty() || !chr.summons.containsValue(it)) {
                            objectWLock.lock()
                            try { mapObjects.remove(k) } finally { objectWLock.unlock() }
                            return@forEachIndexed
                        }
                    }
                }
                if (isNonRangedType(it.objectType)) {
                    it.sendSpawnData(c)
                    if (it.objectType == MapObjectType.PLAYER) {
                        it as Character
                        val allDiseases = it.allDiseases
                        if (allDiseases.isNotEmpty()) {
                            allDiseases.values.forEach { d ->
                                broadcastMessage(it, CharacterPacket.giveForeignDeBuff(it.id, d.debuff, d.skill), false)
                            }
                        }
                    }
                } else if (it.objectType == MapObjectType.MONSTER) updateMonsterController(it as Monster)
            }
        } finally {
            objectRLock.unlock()
        }
        if (chr != null) {
            getMapObjectsInRange(chr.position, 722500.0, rangedMapObjectTypes).forEach {
                if (it.objectType == MapObjectType.REACTOR) {
                    it as Reactor
                    if (it.alive) {
                        it.sendSpawnData(chr.client)
                        chr.addVisibleMapObject(it)
                    }
                } else {
                    it.sendSpawnData(chr.client)
                    chr.addVisibleMapObject(it)
                }
            }
        }
    }



    interface DelayedPacketCreation {
        fun sendPackets(c: Client)
    }

    interface SpawnCondition {
        fun canSpawn(chr: Character): Boolean
    }

    private inner class ExpireMapItemJob(val mapItem: MapItem) : Runnable {
        override fun run() {
            if (mapItem == mapObjects[mapItem.objectId]) {
                mapItem.itemLock.lock()
                try {
                    if (mapItem.pickedUp) return
                    broadcastMessage(GameplayPacket.removeItemFromMap(mapItem.objectId, 0, 0), mapItem.position)
                    mapItem.pickedUp = true
                } finally {
                    mapItem.itemLock.unlock()
                    removeMapObject(mapItem)
                }
            }
        }
    }

    private inner class ActivateItemReactor(val mapItem: MapItem, val reactor: Reactor, val c: Client) : Runnable {
        override fun run() {
            if (mapItem == mapObjects[mapItem.objectId]) {
                mapItem.itemLock.lock()
                try {
                    if (mapItem.pickedUp) return
                    broadcastMessage(GameplayPacket.removeItemFromMap(mapItem.objectId, 0, 0), mapItem.position)
                    removeMapObject(mapItem)
                    reactor.hitReactor(c, 0, 0, 0)
                    reactor.timerActive = false
                    if (reactor.delay > 0) {
                        CoroutineManager.schedule({
                            reactor.state = 0
                            broadcastMessage(GameplayPacket.triggerReactor(reactor, 0))
                        }, reactor.delay.toLong())
                    }
                } finally {
                    mapItem.itemLock.unlock()
                }
            }
        }
    }

    companion object : KLogging() {
        val rangedMapObjectTypes = listOf(MapObjectType.SHOP, MapObjectType.ITEM, MapObjectType.NPC, MapObjectType.MONSTER, MapObjectType.DOOR, MapObjectType.SUMMON, MapObjectType.REACTOR)
    }
}