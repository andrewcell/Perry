package client

import client.autoban.AutobanFactory
import client.autoban.AutobanManager
import client.inventory.*
import constants.ExpTable
import constants.ItemConstants
import constants.ServerConstants
import constants.skills.*
import database.*
import kotlinx.coroutines.Job
import mu.KLogging
import net.server.PlayerBuffValueHolder
import net.server.PlayerCoolDownValueHolder
import net.server.Server
import net.server.channel.handlers.CashOperationHandler
import net.server.guild.Guild
import net.server.guild.GuildCharacter
import net.server.world.Messenger
import net.server.world.Party
import net.server.world.PartyCharacter
import net.server.world.PartyOperation
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import scripting.event.EventInstanceManager
import server.*
import server.InventoryManipulator.Companion.removeById
import server.ItemInformationProvider.getInventoryType
import server.events.Events
import server.events.RescueGaga
import server.events.gm.Fitness
import server.events.gm.Ola
import server.life.MobSkill
import server.life.Monster
import server.maps.*
import server.partyquest.PartyQuest
import server.quest.Quest
import tools.CoroutineManager
import tools.PacketCreator
import tools.packet.*
import java.awt.Point
import java.lang.ref.WeakReference
import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp
import java.text.DecimalFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class Character(
    var world: Int,
    val accountId: Int,
    var map: GameMap,
    var client: Client,
) : AbstractAnimatedMapObject() {
    var loggedIn = false
    var attackTick = 0
    var bookCover = 0
    var markedMonster = 0
    var battleshipHp = 0
    var chalkBoard = ""
    var mesosTraded = 0
    var expRate = 2
    var mesoRate = 1
    var dropRate = 1
    var possibleReports = 10
    var initialSpawnPoint = 0
    var lastUsedCashItem = 0L
    var notMovePlayer = false
    var lastFameTime = 0
    var messengerPosition = 4
    var canDoor = true
    private var pendantExp = 0
    var guildId = -1
        set(value) {
            field = value
            if (field > 0) {
                if (mgc == null) GuildCharacter(this) else mgc?.guildId = guildId
            }
        }
    var itemEffect = 0
    var slots = -1
    private var mPoints = 0
    var energybar = 0
    var conversation = 0
    var chair = 0
    var remainingAp = 0
    var remainingSp = 0
    var hpMpApUsed = 0
    var mapId = 0
    var hidden = false
    var channelCheck = false
    var banned = false
    private var keyValueChanged = false
    var headTitle = 0
    var linkedLevel = 0
    var currentPage = 0
    var currentType = 0
    var currentTab = 1
    var ci = 0
    var search: String? = null
    var rank = 1
    var rankMove = 0
    var jobRank = 1
    var jobRankMove = 0
    var omokWins = 0
    var omokLooses = 0
    var omokTies = 0
    var matchCardWins = 0
    var matchCardLosses = 0
    var matchCardTies = 0
    var merchantMeso = 0
        set(value) {
            try {
                transaction {
                    Characters.update({ Characters.id eq this@Character.id }) {
                        it[merchantMesos] = value
                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to update merchant mesos in database. CharacterId: $id" }
            }
            field = value
        }
    var hasMerchant = false
        set(value) {
            try {
                transaction {
                    Characters.update({ Characters.id eq this@Character.id }) {
                        it[hasMerchant] = value
                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to update has merchant in database. CharacterId: $id" }
            }
            field = value
        }
    var timeSet = 0
    var hair = 0
    var curse = false
    var linkedName = ""

    var hp = 5
    var mp = 5

    var localMaxHp = 0
    var localMaxMp = 0
    private var localStr = 0
    private var localDex = 0
    var localLuk = 0
    private var localInt = 0
    var id: Int = 0
    var name: String = ""
    var level: Short = 1
    var fame: Int = 0
    var str: Int = 4
        set(value) {
            field = value
            recalcLocalStats()
        }
    var dex: Int = 4
        set(value) {
            field = value
            recalcLocalStats()
        }
    var int: Int = 4
        set(value) {
            field = value
            recalcLocalStats()
        }
    var luk: Int = 4
        set(value) {
            field = value
            recalcLocalStats()
        }
    var maxHp: Int = 50
        set(value) {
            field = value
            recalcLocalStats()
        }
    var maxMp: Int = 5
        set(value) {
            field = value
            recalcLocalStats()
        }
    var gmLevel: Int = 0
    var magic: Int = 0
    var watk = 0
    var gender = 0
    var guildRank = 0
        set(value) {
            field = value
            mgc?.guildRank = value
        }
    var petAutoHp = 0
    var petAutoMp = 0
    var clone = false
    var eventTeam = 0
    var face = 0
    private var cp = 0
    var obtainedCp = 0
    var portalDelay = 0L

    var autoban = AutobanManager(this)
    var cashShop: CashShop? = null
    var exp = AtomicInteger()
    var fitness: Fitness? = null
    var gachaExp = AtomicInteger()
    var meso = AtomicInteger()
    var eventInstance: EventInstanceManager? = null
    var hiredMerchant: HiredMerchant? = null
    var messenger: Messenger? = null
    var mgc: GuildCharacter? = null
    var mpc: PartyCharacter? = null
    var miniGame: MiniGame? = null
    var mount: Mount? = null
    var party: Party? = null
        set(value) {
            if (value == null) mpc = null
            field = value
        }
    var partyQuest: PartyQuest? = null
    var pet: Pet? = null
    var playerShop: PlayerShop? = null
    var shop: Shop? = null
    var ola: Ola? = null
    var buddyList: BuddyList = BuddyList(20)
    val storage = Storage.loadOrCreateFromDb(accountId, world)
    var skinColor = SkinColor.NORMAL
    var trade: Trade? = null
    var monsterBook = MonsterBook()

    private val inventories = arrayOfNulls<Inventory>(InventoryType.values().size)
    val savedLocations = arrayOfNulls<SavedLocation>(SavedLocationType.values().size)
    val trockMaps = arrayOf(999999999, 999999999, 999999999, 999999999, 999999999)
    val vipTrockMaps = arrayOf(
        999999999,
        999999999,
        999999999,
        999999999,
        999999999,
        999999999,
        999999999,
        999999999,
        999999999,
        999999999
    )

    val areaInfos = mutableMapOf<Short, String>()
    val blockedPortals = mutableListOf<String>()
    val crushRings = mutableListOf<Ring>()
    val doors = mutableListOf<Door>()
    private val excluded = mutableListOf<Int>()
    val friendshipRings = mutableListOf<Ring>()
    var lastMonthFameIds = mutableListOf<Int>()
    private val timers = mutableListOf<Job>()
    val controlledMonsters = mutableSetOf<Monster>()
    val visibleMapObjects = mutableSetOf<MapObject>()

    val allDiseases = mutableMapOf<Disease, AllDiseaseValueHolder>()
    private val coolDowns = mutableMapOf<Int, CoolDownValueHolder>()
    private val customValues = mutableMapOf<String, String>()
    private val diseases = mutableMapOf<Disease, DiseaseValueHolder>()
    private val effects = mutableMapOf<BuffStat, BuffStatValueHolder>()
    val entered = mutableMapOf<Int, String>()
    var events = mutableMapOf<String, Events>()
    val keymap = mutableMapOf<Int, KeyBinding>()
    val quests = mutableMapOf<Quest, QuestStatus>()
    val skills = mutableMapOf<Skill, SkillEntry>()
    val summons = mutableMapOf<Int, Summon>()

    private var beholderBuffSchedule: Job? = null
    private var beholderHealingSchedule: Job? = null
    private var berserkSchedule: Job? = null
    private var dragonBloodSchedule: Job? = null
    private var expireTask: Job? = null
    private var fullnessSchedule: Job? = null
    private var hpDecreaseTask: Job? = null
    private var mapTimeLimitTask: Job? = null
    private var mobHackParser: Job? = null
    private var pendantOfSpirit: Job? = null
    private var recoveryTask: Job? = null
    var job = GameJob.BEGINNER
    val nf = DecimalFormat("#,###,###,###")

    init {
        InventoryType.values().forEach {
            val b = if (it == InventoryType.CASH) 96 else 24
            inventories[it.ordinal] = Inventory(it, b.toByte())
        }
        position = Point(0, 0)
    }

    fun addCoolDown(skillId: Int, startTime: Long, length: Long, timer: Job?) {
        if (coolDowns.containsKey(skillId)) coolDowns.remove(skillId)
        coolDowns[skillId] = CoolDownValueHolder(skillId, startTime, length, timer)
    }

    fun addCrushRing(r: Ring) = crushRings.add(r)

    fun addDoor(door: Door) = doors.add(door)

    fun addExcluded(x: Int) = excluded.add(x)

    fun addFame(x: Int) {
        fame += x
    }

    fun addHp(delta: Int) {
        setHpNormal(hp + delta)
        updateSingleStat(CharacterStat.HP, hp)
    }

    fun addMerchantMesos(add: Int) {
        try {
            Characters.update({ Characters.id eq this@Character.id }) {
                it[merchantMesos] = merchantMeso + add
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to add merchant mesos in database. CharacterId: $id" }
        }
        merchantMeso += add
    }

    fun addMp(delta: Int) {
        setMpNormal(mp + delta)
        updateSingleStat(CharacterStat.MP, mp)
    }

    fun addMpHp(hpDiff: Int, mpDiff: Int) {
        setHpNormal(hp + hpDiff)
        setMpNormal(mp + mpDiff)
        updateSingleStat(CharacterStat.HP, hp)
        updateSingleStat(CharacterStat.MP, mp)
    }

    fun addMesosTraded(gain: Int) {
        mesosTraded += gain
    }

    fun addStat(type: Int, up: Int) {
        when (type) {
            1 -> {
                str += up
                updateSingleStat(CharacterStat.STR, str)
            }

            2 -> {
                dex += up
                updateSingleStat(CharacterStat.DEX, dex)
            }

            3 -> {
                int += up
                updateSingleStat(CharacterStat.INT, int)
            }

            4 -> {
                luk += up
                updateSingleStat(CharacterStat.LUK, luk)
            }

            else -> return
        }
    }

    fun addSummon(id: Int, summon: Summon) = summons.put(id, summon)

    fun addTrockMap() {
        if (getTrockSize() <= 4) {
            trockMaps[getTrockSize()] = mapId
        }
    }

    fun addVipTrockMap() {
        if (getVipTrockSize() <= 9) {
            vipTrockMaps[getVipTrockSize()] = mapId
        }
    }

    fun addVisibleMapObject(mo: MapObject) = visibleMapObjects.add(mo)

    fun announce(packet: ByteArray) = client.announce(packet)

    fun autoban(reason: String, gReason: Int) {
        val cal = Calendar.getInstance()
        cal[cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DAY_OF_MONTH], cal[Calendar.HOUR_OF_DAY]] =
            cal[Calendar.MINUTE]
        val ts = Timestamp(cal.timeInMillis)
        try {
            Accounts.update({ Accounts.id eq accountId }) {
                it[banReason] = reason
                it[tempBan] = ts.toInstant()
                it[Accounts.gReason] = gReason
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to update ban information in database. CharacterId: $id" }
        }
    }

    fun ban(reason: String) {
        try {
            Accounts.update({ Accounts.id eq accountId }) {
                it[banned] = true
                it[banReason] = reason
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to update ban information in database. CharacterId: $id" }
        }
    }

    fun block(reason: Int, days: Int, desc: String) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, days)
        val ts = Timestamp(cal.timeInMillis)
        try {
            Accounts.update({ Accounts.id eq accountId }) {
                it[banReason] = desc
                it[tempBan] = ts.toInstant()
                it[gReason] = reason
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to update ban information in database. CharacterId: $id" }
        }
    }

    fun calculateMaxBaseDamage(watk: Int): Int {
        var maxBasedDamage = 1
        if (watk != 0) {
            val weaponItem = getInventory(InventoryType.EQUIPPED)?.getItem(-11)
            maxBasedDamage = if (weaponItem != null) {
                val weapon = ItemInformationProvider.getWeaponType(weaponItem.itemId)
                val statPair = if (weapon == WeaponType.BOW || weapon == WeaponType.CROSSBOW) {
                    Pair(localDex, localStr)
                } else if (job.isA(GameJob.THIEF) && (weapon == WeaponType.CLAW || weapon == WeaponType.DAGGER)) {
                    Pair(localLuk, localDex + localStr)
                } else {
                    Pair(localStr, localDex)
                }
                ((((weapon.maxDamageMultiplier * statPair.first + statPair.second) / 100.0) * watk) + 10).toInt()
            } else {
                0
            }
        }
        return maxBasedDamage
    }

    fun calculateMaxBaseDamageMagic(matk: Int): Int {
        var maxBasedDamage = 1
        if (magic != 0) {
            val weaponItem = getInventory(InventoryType.EQUIPPED)?.getItem(-11)
            maxBasedDamage = if (weaponItem != null) {
                val weapon = ItemInformationProvider.getWeaponType(weaponItem.itemId)
                val pair = if (weapon == WeaponType.WAND || weapon == WeaponType.STAFF) {
                    Pair(localInt, localLuk)
                } else Pair(localInt, localLuk)
                ((((weapon.maxDamageMultiplier * pair.first + pair.second) / 100.0) * magic) + 10).toInt()
            } else {
                0
            }
        }
        return maxBasedDamage
    }

    fun cancelAllBuffs(disconnect: Boolean) {
        if (disconnect) effects.clear()
        else {
            effects.values.forEach {
                cancelEffect(it.effect, false, it.startTime)
            }
        }
    }

    fun cancelBuffStats(stat: BuffStat) {
        val buffStatList = listOf(stat)
        deregisterBuffStats(buffStatList)
        cancelPlayerBuffs(buffStatList)
    }

    fun cancelBuffEffects() {
        effects.values.forEach { it.schedule.cancel() }
        effects.clear()
    }

    fun cancelEffect(itemId: Int) = cancelEffect(ItemInformationProvider.getItemEffect(itemId), false, -1)

    fun cancelEffect(effect: StatEffect?, overwrite: Boolean, startTime: Long) {
        if (effect == null) return
        val buffStats = if (!overwrite) getBuffStats(effect, startTime).toMutableList() else mutableListOf()
        if (overwrite) {
            effect.statUps?.forEach { buffStats.add(it.first) }
        }
        deregisterBuffStats(buffStats)
        if (effect.isMagicDoor()) {
            if (doors.isNotEmpty()) {
                val door = doors.iterator().next()
                door.target.characters.forEach { door.sendDestroyData(it.client) }
                door.town.characters.forEach { door.sendDestroyData(it.client) }
                doors.forEach {
                    door.target.removeMapObject(it)
                    door.town.removeMapObject(it)
                }
                clearDoors()
                silentPartyUpdate()
            }
        }
        if (effect.sourceId == Spearman.HYPER_BODY || effect.sourceId == SuperGM.HYPER_BODY) {
            val statUps = listOf(
                Pair(CharacterStat.HP, min(hp, maxHp)),
                Pair(CharacterStat.MP, min(mp, maxMp)),
                Pair(CharacterStat.MAXHP, maxHp),
                Pair(CharacterStat.MAXHP, maxMp),
            )
            client.announce(CharacterPacket.updatePlayerStats(statUps))
        }
        if (effect.isMonsterRiding()) {
            if (effect.sourceId != Corsair.BATTLE_SHIP) {
                mount?.cancelSchedule()
                mount?.isActive = false
            }
        }
        if (!overwrite) cancelPlayerBuffs(buffStats)
    }

    fun cancelEffectFromBuffStat(stat: BuffStat) {
        val effect = effects[stat]
        effect?.let { cancelEffect(it.effect, false, -1) }
    }

    fun cancelMagicDoor() {
        effects.values.forEach {
            if (it.effect.isMagicDoor()) cancelEffect(it.effect, false, it.startTime)
        }
    }

    fun cancelMapTimeLimitTask() = mapTimeLimitTask?.cancel()

    private fun cancelPlayerBuffs(buffStats: List<BuffStat>) {
        if (client.getChannelServer().players.getCharacterById(id) != null) {
            recalcLocalStats()
            enforceMaxHpMp()
            client.announce(CharacterPacket.cancelBuff(buffStats))
            if (buffStats.isNotEmpty()) {
                map.broadcastMessage(this, CharacterPacket.cancelForeignBuff(id, buffStats), false)
            }
        }
    }

    fun canGiveFame(from: Character): FameStats {
        return when {
            gmLevel > 0 -> FameStats.OK
            lastFameTime >= System.currentTimeMillis() - 3600000 * 24 -> FameStats.NOT_TODAY
            lastMonthFameIds.contains(from.id) -> FameStats.NOT_THIS_MONTH
            else -> FameStats.OK
        }
    }

    fun changeJob(job: GameJob) {
        this.job = job
        this.remainingSp++
        if (job.id % 10 == 2) remainingSp += 2
        if (job.id % 10 > 1) remainingSp += 5
        val jobId = job.id % 1000
        when {
            jobId == 100 -> maxHp += rand(200, 250)
            jobId == 200 -> maxMp += rand(100, 150)
            jobId % 100 == 0 -> {
                maxHp += rand(100, 150)
                maxHp += rand(25, 50)
            }

            jobId in 1..199 -> maxHp += rand(300, 350)
            jobId < 300 -> maxMp += rand(450, 500)
            jobId > 0 && jobId != 1000 -> {
                maxHp += rand(300, 350)
                maxMp += rand(150, 200)
            }

            else -> {}
        }
        if (maxHp >= 30000) maxHp = 30000
        if (maxMp >= 30000) maxMp = 30000
        if (!isGM()) {
            for (i in 1..4) {
                gainSlots(i, 4, true)
            }
        }
        val statUp = listOf(
            Pair(CharacterStat.MAXHP, maxHp),
            Pair(CharacterStat.MAXMP, maxMp),
            Pair(CharacterStat.AVAILABLEAP, remainingAp),
            Pair(CharacterStat.AVAILABLESP, remainingSp),
            Pair(CharacterStat.JOB, job.id)
        )
        recalcLocalStats()
        client.announce(CharacterPacket.updatePlayerStats(statUp))
        silentPartyUpdate()
        getGuild()?.broadcast(GuildPacket.jobMessageToGuild(0, job.id, name), id, Guild.BCOp.NONE)
        guildUpdate()
        map.broadcastMessage(this, CharacterPacket.showForeignEffect(id, 8), false)
    }

    fun changeKeyBinding(key: Int, binding: KeyBinding) {
        if (binding.type != 0) {
            keymap[key] = binding
        } else {
            keymap.remove(key)
        }
    }

    fun changeMap(mapId: Int, portal: Int = 0) {
        val map = client.getChannelServer().mapFactory.getMap(mapId)
        changeMap(map, map.getPortal(portal) ?: return)
    }

    fun changeMap(mapId: Int, portal: String) {
        val map = client.getChannelServer().mapFactory.getMap(mapId)
        changeMap(map, map.getPortal(portal) ?: return)
    }

    fun changeMap(mapId: Int, portal: Portal) {
        val map = client.getChannelServer().mapFactory.getMap(mapId)
        changeMap(map, portal)
    }

    fun changeMap(to: GameMap, pto: Portal? = to.getPortal(0)) {
        if (pto == null) return
        pto.position?.let { changeMapInternal(to, it, PacketCreator.getWarpToMap(to, pto.id, this)) }
    }

    fun changeMap(to: GameMap, pos: Point) {
        changeMapInternal(to, pos, PacketCreator.getWarpToMap(to, 0x80, this))
    }

    fun changeMapBanish(mapId: Int, portal: String, message: String) {
        dropMessage(5, message)
        val map = client.getChannelServer().mapFactory.getMap(mapId)
        changeMap(map, map.getPortal(portal))
    }

    private fun changeMapInternal(to: GameMap, pos: Point, warpPacket: ByteArray) {
        client.announce(warpPacket)
        map.removePlayer(this)
        if (client.getChannelServer().players.getCharacterById(id) != null) {
            map = to
            position = pos
            map.addPlayer(this)
            if (party != null) {
                mpc?.mapId = to.mapId
                silentPartyUpdate()
                client.announce(InteractPacket.updateParty(client.channel, party, PartyOperation.SILENT_UPDATE, null))
                updatePartyMemberHp()
            }
            if (map.hpDec > 0) {
                hpDecreaseTask = CoroutineManager.schedule({ doHurtHp() }, 10000)
            }
        }
    }

    fun changeSkillLevel(skill: Skill, newLevel: Byte, newMasterLevel: Int, expiration: Long) {
        if (newLevel > -1) {
            skills[skill] = SkillEntry(newLevel, newMasterLevel, expiration.toLong())
            client.announce(CharacterPacket.updateSkill(skill.id, newLevel.toInt()))
        } else {
            skills.remove(skill)
            client.announce(CharacterPacket.updateSkill(skill.id, newLevel.toInt()))
            try {
                transaction {
                    Skills.deleteWhere { (Skills.skillId eq skill.id) and (Skills.characterId eq this@Character.id) }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to delete skill in database. CharacterId: $id" }
            }
        }
    }

    fun disconnected() = mobHackParser?.cancel()

    private fun checkMobPosition() {
        val x = super.position.x
        var count = 0
        map.allMonster().forEach {
            var d = x - it.position.x
            if (d < 0) d = -d
            if (d < 30) count++
        }
        if (count >= 555) {
            autoban.addPoint(AutobanFactory.MOVE_MONSTER, "Mob: $count")
            map.fired()
        }
    }

    fun checkBerserk() {
        berserkSchedule?.cancel()
        val chr = this
        if (job == GameJob.DARKKNIGHT) {
            val berserkX = SkillFactory.getSkill(DarkKnight.BERSERK)
            val skillLevel = berserkX?.let { getSkillLevel(it) } ?: 0
            if (skillLevel > 0) {
                val berserk = chr.hp * 1000 / chr.maxHp < (berserkX?.getEffect(skillLevel.toInt())?.x ?: 1)
                berserkSchedule = CoroutineManager.register({
                    client.announce(CharacterPacket.showOwnBerserk(skillLevel.toInt(), berserk))
                    map.broadcastMessage(this, PacketCreator.showBerserk(id, skillLevel.toInt(), berserk), false)
                }, 5000, 3000)
            }
        }
    }

    fun checkMessenger() {
        if (messenger != null && messengerPosition in 0..3) {
            messenger?.let { Server.getWorld(world).updateMessenger(it.id, name, client.channel) }
        }
    }

    fun checkMonsterAggro(monster: Monster) {
        if (!monster.controllerHasAggro) {
            if ((monster.controller?.get() ?: return) == this) {
                monster.controllerHasAggro = true
            } else {
                monster.switchController(this, true)
            }
        }
    }

    private fun clearDoors() = doors.clear()

    fun clearSavedLocation(type: SavedLocationType) = savedLocations.set(type.ordinal, null)

    fun controlMonster(monster: Monster, aggro: Boolean) {
        monster.controller = WeakReference(this)
        controlledMonsters.add(monster)
        client.announce(GameplayPacket.controlMonster(monster, false, aggro))
    }

    fun countItem(itemId: Int) = inventories[getInventoryType(itemId).ordinal]?.countById(itemId) ?: 0

    fun decreaseBattleshipHp(d: Int) {
        battleshipHp -= d
        if (battleshipHp <= 0) {
            battleshipHp = 0
            val coolDown = SkillFactory.getSkill(Corsair.BATTLE_SHIP)?.coolTime ?: 0
            announce(CharacterPacket.skillCoolDown(Corsair.BATTLE_SHIP, coolDown))
            addCoolDown(
                Corsair.BATTLE_SHIP, System.currentTimeMillis(), coolDown.toLong(),
                CoroutineManager.schedule(CancelCoolDownAction(this, Corsair.BATTLE_SHIP), coolDown * 1000L)
            )
            removeCoolDown(5221999)
            cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING)
        } else {
            announce(CharacterPacket.skillCoolDown(5221999, battleshipHp / 10))
            addCoolDown(5221999, 0, battleshipHp.toLong(), null)
        }
    }

    fun decreaseReports() = possibleReports--

    fun deleteFromTrockMaps(map: Int) {
        trockMaps.forEachIndexed { index, v ->
            if (v == map) {
                trockMaps[index] = 999999999
            }
        }
    }

    fun deleteFromVipTrockMaps(map: Int) {
        vipTrockMaps.forEachIndexed { index, v ->
            if (v == map) {
                vipTrockMaps[index] = 999999999
            }
        }
    }

    fun deleteGuild(guildId: Int) {
        try {
            Characters.update({ Characters.guildId eq guildId }) {
                it[Characters.guildId] = 0
                it[guildRank] = 0
            }
            Guilds.deleteWhere { Guilds.guildId eq this@Character.guildId }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to delete guild to character. CharacterId: $id" }
        }
    }

    @Throws(SQLException::class)
    fun deleteWhereCharacterId(con: Connection, sql: String) {
        val ps = con.prepareStatement(sql)
        ps.setInt(1, id)
        ps.executeUpdate()
    }

    private fun deregisterBuffStats(stats: List<BuffStat>) {
        synchronized(stats) {
            val effectsToCancel = mutableListOf<BuffStatValueHolder>()
            stats.forEach { stat ->
                val mbsvh = effects[stat] ?: return@forEach
                effects.remove(stat)
                var addMbsvh = true
                effectsToCancel.forEach {
                    if (mbsvh.startTime == it.startTime && it.effect == mbsvh.effect) addMbsvh = false
                }
                if (addMbsvh) effectsToCancel.add(mbsvh)
                when (stat) {
                    BuffStat.RECOVERY -> {
                        recoveryTask?.cancel()
                        recoveryTask = null
                    }

                    BuffStat.SUMMON, BuffStat.PUPPET -> {
                        val summonId = mbsvh.effect.sourceId
                        val summon = summons[summonId]
                        if (summon != null) {
                            map.broadcastMessage(GameplayPacket.removeSummon(summon, true), summon.position)
                            map.removeMapObject(summon)
                            removeVisibleMapObject(summon)
                            summons.remove(summonId)
                        }
                        if (summon?.skill == DarkKnight.BEHOLDER) {
                            beholderHealingSchedule?.cancel()
                            beholderHealingSchedule = null
                            beholderBuffSchedule?.cancel()
                            beholderBuffSchedule = null
                        }
                    }

                    BuffStat.DRAGONBLOOD -> {
                        dragonBloodSchedule?.cancel()
                        dragonBloodSchedule = null
                    }

                    else -> {}
                }
            }
            effectsToCancel.forEach { it.schedule.cancel() }
        }
    }

    fun disableDoor() {
        canDoor = false
        CoroutineManager.schedule({ canDoor = true }, 5000)
    }

    fun dispel() {
        effects.values.forEach {
            if (it.effect.skill) cancelEffect(it.effect, false, it.startTime)
        }
    }

    fun dispelSkill(skillId: Int) {
        effects.values.forEach {
            if (skillId == 0) {
                if (it.effect.skill && (it.effect.sourceId % 10000000 == 1004) || dispelSkills(it.effect.sourceId)) {
                    cancelEffect(it.effect, false, it.startTime)
                }
            } else if (it.effect.skill && it.effect.sourceId == skillId) {
                cancelEffect(it.effect, false, it.startTime)
            }
        }
    }

    private fun dispelSkills(skillId: Int): Boolean {
        return when (skillId) {
            DarkKnight.BEHOLDER, FPArchMage.ELQUINES, ILArchMage.IFRIT,
            Priest.SUMMON_DRAGON, Bishop.BAHAMUT, Ranger.PUPPET,
            Ranger.SILVER_HAWK, Sniper.PUPPET, Sniper.GOLDEN_EAGLE, Hermit.SHADOW_PARTNER -> true

            else -> false
        }
    }

    private fun doHurtHp() {
        if (map.hpDecProtect.let { getInventory(InventoryType.EQUIPPED)?.findById(it) } != null) {
            return
        }
        addHp(-map.hpDec)
        hpDecreaseTask = CoroutineManager.schedule({
            doHurtHp()
        }, 10000)
    }

    fun dropMessage(type: Int = 0, message: String) = client.announce(InteractPacket.serverNotice(type, message))

    private fun enforceMaxHpMp() {
        val stats = mutableListOf<Pair<CharacterStat, Int>>()
        if (mp > localMaxMp) {
            mp = mp
            stats.add(Pair(CharacterStat.MP, mp))
        }
        if (hp > localMaxHp) {
            hp = hp
            stats.add(Pair(CharacterStat.HP, hp))
        }
        if (stats.size > 0) {
            client.announce(CharacterPacket.updatePlayerStats(stats))
        }
    }

    fun empty(remove: Boolean) {
        dragonBloodSchedule?.cancel()
        hpDecreaseTask?.cancel()
        beholderBuffSchedule?.cancel()
        beholderHealingSchedule?.cancel()
        berserkSchedule?.cancel()
        recoveryTask?.cancel()
        cancelExpirationTask()
        timers.forEach { it.cancel() }
        timers.clear()
        mount?.empty()
        mount = null
        if (remove) {
            mpc = null
            mgc = null
            party = null
        }
    }

    fun enteredScript(script: String, mapId: Int) = entered.putIfAbsent(mapId, script)

    fun equipChanged() {
        map.broadcastMessage(this, CharacterPacket.updateCharLook(this), false)
        recalcLocalStats()
        enforceMaxHpMp()
        messenger?.let { Server.getWorld(world).updateMessenger(it, name, client.channel) }
    }

    fun equipPendantOfSpirit() {
        pendantOfSpirit = CoroutineManager.register({
            if (pendantExp < 3) {
                pendantExp++
                message("Pendant of the Spirit has been equipped for $pendantExp hour(s), you will now receive ${pendantExp}0% bonus exp.")
            } else {
                pendantOfSpirit?.cancel()
            }
        }, 3600000, 0)
    }

    fun cancelExpirationTask() {
        expireTask?.cancel()
        expireTask = null
    }

    fun expirationTask() {
        if (expireTask == null) {
            expireTask = CoroutineManager.register({
                val expiration: Long
                val currentTime = System.currentTimeMillis()
                val i: Iterator<Skill> = skills.keys.iterator()
                while (i.hasNext()) {
                    val key = i.next()
                    val (_, _, expiration1) = skills[key] ?: continue
                    if (expiration1 != -1L && expiration1 < currentTime) {
                        changeSkillLevel(key, (-1).toByte(), 0, -1)
                    }
                }
                val toBeRemove = mutableListOf<Item>()
                inventories.forEach { inv ->
                    if (inv == null) return@forEach
                    inv.list().forEach { i ->
                        if (i.expiration.toInt() != -1 && (i.expiration < currentTime) && ((i.flag and ItemConstants.LOCK) == ItemConstants.LOCK)) {
                            var aids = i.flag
                            aids = aids and ItemConstants.LOCK.inv()
                            i.flag = aids
                            i.expiration = -1
                            forceUpdateItem(i)
                        } else if (i.expiration.toInt() != -1 && i.expiration < currentTime) {
                            client.announce(ItemPacket.itemExpired(i.itemId))
                            toBeRemove.add(i)
                        }
                    }
                    toBeRemove.forEach {
                        InventoryManipulator.removeFromSlot(
                            client, inv.type, it.position, it.quantity,
                            fromDrop = true,
                            consume = false
                        )
                    }
                    toBeRemove.clear()
                }
            }, 0, 60000)
        }
    }

    fun forceUpdateItem(item: Item) {
        val mods = mutableListOf(ModifyInventory(3, item))
        if (item.itemId / 10000 == 207 || item.quantity > 0) mods.add(ModifyInventory(0, item))
        client.announce(CharacterPacket.modifyInventory(true, mods.toList()))
    }

    fun gainGachaExp(gain: Int) = updateSingleStat(CharacterStat.EXP, exp.addAndGet(gain))

    fun gainExp(gain: Int, show: Boolean, inChat: Boolean, white: Boolean = true) {
        val equip = (gain * expRate / 10) * pendantExp
        var total = gain + equip
        if (level < 200) {
            if (exp.get() + total > Integer.MAX_VALUE) {
                val gainFirst = ExpTable.getExpNeededForLevel(level.toInt()) - exp.get()
                total -= gainFirst + 1
                gainExp(gainFirst + 1, false, inChat, white)
            }
            updateSingleStat(CharacterStat.EXP, exp.addAndGet(total))
            if (show && gain != 0) {
                client.announce(CharacterPacket.getShowExpGain(gain, equip, inChat, white))
            }
            if (exp.get() >= ExpTable.getExpNeededForLevel(level.toInt())) {
                levelUp(true)
                val need = ExpTable.getExpNeededForLevel(level.toInt())
                if (exp.get() >= need) {
                    exp.set(need - 1)
                    updateSingleStat(CharacterStat.EXP, need)
                }
            }
        }
    }

    fun gainFame(delta: Int) {
        fame += delta
        updateSingleStat(CharacterStat.FAME, fame)
    }

    fun gainMeso(gain: Int, show: Boolean, enableActions: Boolean = false, inChat: Boolean = false) {
        if (meso.get() + gain < 0) {
            client.announce(PacketCreator.enableActions())
            return
        }
        updateSingleStat(CharacterStat.MESO, meso.addAndGet(gain), enableActions)
        if (show) client.announce(CharacterPacket.getShowMesoGain(gain, inChat))
    }

    fun gainMPoint(quantity: Int) {
        mPoints += quantity
        dropMessage(5, "$quantity 메이플 포인트를 받으셨습니다.")
    }

    fun gainSlots(type: Int, slots: Int, update: Boolean = true): Boolean {
        val slot = slots + (inventories[type]?.slotLimit ?: 0)
        if (slot <= 96) {
            inventories[type]?.slotLimit = slot.toByte()
            saveToDatabase()
            if (update) client.announce(CharacterPacket.updateInventorySlotLimit(type, slot))
            return true
        }
        return false
    }

    fun getAllBuffs(): List<PlayerBuffValueHolder> {
        val ret = mutableListOf<PlayerBuffValueHolder>()
        effects.values.forEach { ret.add(PlayerBuffValueHolder(it.startTime, it.effect)) }
        return ret
    }

    fun getAllCoolDowns(): List<PlayerCoolDownValueHolder> {
        val ret = mutableListOf<PlayerCoolDownValueHolder>()
        coolDowns.values.forEach { ret.add(PlayerCoolDownValueHolder(it.skillId, it.startTime, it.length)) }
        return ret
    }

    fun getAndRemoveCp(): Int {
        var rCp = 10
        if (cp < 9) {
            rCp = cp
            cp = 0
        } else {
            cp -= 10
        }
        return rCp
    }

    fun getBuffedStartTime(effect: BuffStat) = effects[effect]?.startTime

    fun getBuffedValue(effect: BuffStat) = effects[effect]?.value

    fun getBuffSource(stat: BuffStat) = effects[stat]?.effect?.sourceId ?: -1

    private fun getBuffStats(effect: StatEffect, startTime: Long) = effects.filter {
        it.value.effect.sameSource(effect) && (startTime == -1L || startTime == it.value.startTime)
    }.keys.toList()

    fun getCompletedQuests() = quests.values.filter { it.status == QuestStatus.Status.COMPLETED }

    fun getCrushRingsSorted() = crushRings.sorted()

    fun getGuild() = Server.getGuild(guildId)

    fun getItemQuantity(itemId: Int, checkEquipped: Boolean): Int {
        var possesed = inventories[getInventoryType(itemId).ordinal]?.countById(itemId) ?: return 0
        if (checkEquipped) {
            possesed += inventories[InventoryType.EQUIPPED.ordinal]?.countById(itemId) ?: return 0
        }
        return possesed
    }

    fun getJobType() = job.id / 1000

    fun getKeyValue(key: String) = customValues[key]

    fun getFh() = map.footholds?.findBelow(position)?.id ?: 0

    fun getMasterLevel(skill: Skill) = skills[skill]?.masterLevel ?: 0

    fun getMiniGamePoints(type: String, omok: Boolean): Int {
        if (omok) {
            when (type) {
                "wins" -> omokWins
                "losses" -> omokLooses
                else -> omokTies
            }
        } else {
            when (type) {
                "wins" -> matchCardWins
                "losses" -> matchCardLosses
                else -> matchCardTies
            }
        }
        return 0
    }

    fun getPartyId() = party?.id ?: -1

    fun getQuestStatus(quest: Int) = quests.values.find { it.quest.id.toInt() == quest }?.status?.id ?: 0

    fun getCustomQuestStatus(questId: Int): Int {
        var status = 0
        try {
            transaction {
                val row = CustomQuests.selectAll().where {
                    CustomQuests.characterId eq this@Character.id
                }
                if (row.empty()) {
                    CustomQuests.insert {
                        it[characterId] = this@Character.id
                        it[CustomQuests.questId] = questId
                        it[CustomQuests.status] = 0
                    }
                } else {
                    row.forEach {
                        if (it[CustomQuests.questId] == questId) {
                            status = it[CustomQuests.status]
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to get custom quest status from database. CharacterId: $id" }
        }
        return status
    }

    fun setCustomQuestStatus(questId: Int, status: Int) {
        try {
            transaction {
                CustomQuests.update({ CustomQuests.characterId eq this@Character.id }) {
                    it[CustomQuests.questId] = questId
                    it[CustomQuests.status] = status
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Faild to update custom quest status to database. CharacterId: $id" }
        }
    }

    fun getQuest(quest: Quest) = quests[quest] ?: QuestStatus(quest, QuestStatus.Status.NOT_STARTED)

    fun needQuestItem(questId: Int, itemId: Int): Boolean {
        if (questId <= 0) return true
        val quest = Quest.getInstance(questId)
        return getInventory(ItemConstants.getInventoryType(itemId))?.countById(itemId)
            ?.let { it < quest.getItemAmountNeeded(itemId) } ?: false
    }

    fun getSavedLocationMapId(type: String) = savedLocations[SavedLocationType.valueOf(type).ordinal]?.mapId

    fun getSkillLevel(skill: Int) = skills[SkillFactory.getSkill(skill)]?.skillLevel ?: 0

    fun getSkillLevel(skill: Skill) = skills[skill]?.skillLevel ?: 0

    fun getSkillExpiration(skill: Int) = skills[SkillFactory.getSkill(skill)]?.expiration ?: -1

    fun getSkillExpiration(skill: Skill) = skills[skill]?.expiration ?: -1

    fun getStartedQuests() = quests.values.filter { it.status == QuestStatus.Status.STARTED }

    fun getStartedQuestsSize() = getStartedQuests().size

    fun getStatForBuff(effect: BuffStat) = effects[effect]?.effect

    fun getSlots(type: Int) = if (type == InventoryType.CASH.type.toInt()) 96 else inventories[type]?.slotLimit

    private fun getTrockSize() = trockMaps.filter { it != 999999999 }.size

    private fun getVipTrockSize() = vipTrockMaps.filter { it != 999999999 }.size

    fun giveCoolDowns(skillId: Int, startTime: Long, length: Long) {
        if (skillId == 5221999) {
            battleshipHp = length.toInt()
            addCoolDown(skillId, 0, length, null)
        } else {
            val time = ((length + startTime) - System.currentTimeMillis())
            addCoolDown(
                skillId, System.currentTimeMillis(), time,
                CoroutineManager.schedule(CancelCoolDownAction(this, skillId), time)
            )
        }
    }

    fun giveDebuff(disease: Disease, skill: MobSkill) {
        val debuff = listOf(Pair(disease, skill.x))
        if (!hasDisease(disease) && diseases.size < 2) {
            CoroutineManager.schedule({
                dispelDeBuff(disease)
            }, skill.duration)
            if (disease == Disease.CURSE) client.player?.curse = true
            allDiseases[disease] = AllDiseaseValueHolder(debuff, skill)
            diseases[disease] = DiseaseValueHolder(System.currentTimeMillis(), skill.duration)
            client.announce(CharacterPacket.giveDeBuff(debuff, skill))
            map.broadcastMessage(this, CharacterPacket.giveForeignDeBuff(id, debuff, skill), false)
        }
    }

    private fun guildUpdate() {
        if (guildId < 1) return
        mgc?.level = level.toInt()
        mgc?.jobId = job.id
        mgc?.let { Server.getGuild(it)?.memberLevelJobUpdate(it) }
    }

    fun handleEnergyChargeGain() {
        val energyCharge = SkillFactory.getSkill(Marauder.ENERGY_CHARGE) ?: return
        val cEffect = energyCharge.getEffect(getSkillLevel(energyCharge).toInt())
        if (energybar < 10000) {
            energybar += 102
            if (energybar > 10000) energybar = 10000
            val stat = listOf(Pair(BuffStat.ENERGY_CHARGE, energybar))
            setBuffedValue(BuffStat.ENERGY_CHARGE, energybar)
            client.announce(CharacterPacket.giveBuff(energybar, 0, stat))
            client.announce(CharacterPacket.showOwnBuffEffect(energyCharge.id, 2))
            map.broadcastMessage(this, PacketCreator.showBuffEffect(id, energyCharge.id, 2))
            map.broadcastMessage(this, CharacterPacket.giveForeignBuff(energybar, stat))
        }
        if (energybar in 10000..10999) {
            energybar = 15000
            CoroutineManager.schedule({
                energybar = 0
                val stat = listOf(Pair(BuffStat.ENERGY_CHARGE, energybar))
                setBuffedValue(BuffStat.ENERGY_CHARGE, energybar)
                client.announce(CharacterPacket.giveBuff(energybar, 0, stat))
                map.broadcastMessage(this, CharacterPacket.giveForeignBuff(energybar, stat))
            }, cEffect.duration.toLong())
        }
    }

    fun handleOrbconsume() {
        val skillId = Crusader.COMBO
        val combo = SkillFactory.getSkill(skillId) ?: return
        val stat = listOf(Pair(BuffStat.COMBO, 1))
        setBuffedValue(BuffStat.COMBO, 1)
        getBuffedStartTime(BuffStat.COMBO)?.minus(System.currentTimeMillis())?.let {
            client.announce(
                CharacterPacket.giveBuff(
                    skillId,
                    (combo.getEffect(getSkillLevel(combo).toInt()).duration + it.toInt()), stat
                )
            )
        }
        map.broadcastMessage(this, CharacterPacket.giveForeignBuff(id, stat), false)
    }

    fun hasEntered(script: String, mapId: Int) = entered[mapId] == script

    fun hasGivenFame(to: Character) {
        lastFameTime = System.currentTimeMillis().toInt()
        lastMonthFameIds.add(to.id)
        try {
            transaction {
                FameLog.insert {
                    it[characterId] = this@Character.id
                    it[characterIdTo] = to.id

                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to update fame log in database. CharacterId: $id" }
        }
    }

    fun haveItem(itemId: Int) = getItemQuantity(itemId, false) > 0

    fun haveItem(itemId: Int, qw: Int) = getItemQuantity(itemId, false) >= qw

    fun isActiveBuffedValue(skillId: Int) =
        effects.values.find { it.effect.skill && it.effect.sourceId == skillId }?.let { true } ?: false

    fun isAlive() = hp > 0

    fun isBuffFrom(stat: BuffStat, skill: Skill): Boolean {
        val e = effects[stat] ?: return false
        return e.effect.skill && e.effect.sourceId == skill.id
    }

    fun isBeginnerJob() = job.id == 0

    fun isMapObjectVisible(mo: MapObject) = visibleMapObjects.contains(mo)

    fun leaveMap() {
        shop = null
        stance = 0
        conversation = 0
        controlledMonsters.clear()
        visibleMapObjects.clear()
        if (chair != 0) chair = 0
        hpDecreaseTask?.cancel()
    }

    fun levelUp(takeExp: Boolean) {
        var improvingMaxHp: Skill? = null
        var improvingMaxMp: Skill? = null
        var improvingMaxHpLevel = 0
        var improvingMaxMpLevel = 0

        remainingAp += 5
        if (level < 70) remainingAp++
        when {
            job == GameJob.BEGINNER -> {
                maxHp += rand(12, 16)
                maxMp += rand(10, 12)
            }

            job.isA(GameJob.WARRIOR) -> {
                improvingMaxHp = SkillFactory.getSkill(Swordsman.IMPROVED_MAX_HP_INCREASE) ?: return
                if (job.isA(GameJob.CRUSADER)) improvingMaxMp = SkillFactory.getSkill(1210000)
                improvingMaxHpLevel = getSkillLevel(improvingMaxHp).toInt()
                maxHp += rand(24, 28)
                maxMp += rand(4, 6)
            }

            job.isA(GameJob.MAGICIAN) -> {
                improvingMaxMp = SkillFactory.getSkill(Magician.IMPROVED_MAX_MP_INCREASE) ?: return
                improvingMaxMpLevel = getSkillLevel(improvingMaxMp).toInt()
                maxHp += rand(10, 14)
                maxHp += rand(22, 24)
            }

            job.isA(GameJob.GM) -> {
                maxHp = 30000
                maxMp = 30000
            }

            job.isA(GameJob.PIRATE) -> {
                improvingMaxHp = SkillFactory.getSkill(5100000) ?: return
                improvingMaxHpLevel = getSkillLevel(improvingMaxHp).toInt()
                maxHp += rand(22, 28)
                maxMp += rand(18, 23)
            }

            else -> {}
        }
        if (improvingMaxHpLevel > 0 && (job.isA(GameJob.WARRIOR) || job.isA(GameJob.PIRATE))) {
            maxHp += improvingMaxHp?.getEffect(improvingMaxHpLevel)?.x ?: 0
        }
        if (improvingMaxMpLevel > 0 && (job.isA(GameJob.MAGICIAN) || job.isA(GameJob.CRUSADER))) {
            maxMp += improvingMaxMp?.getEffect(improvingMaxMpLevel)?.x ?: 0
        }
        maxMp += localInt / 10
        if (takeExp) {
            exp.addAndGet(-ExpTable.getExpNeededForLevel(level.toInt()))
            if (exp.get() < 0) exp.set(0)
        }
        level++
        if (level >= 200) {
            exp.set(0)
        }
        maxHp = min(30000, maxHp)
        maxMp = min(30000, maxMp)
        hp = maxHp
        mp = maxMp
        recalcLocalStats()
        val statUp = mutableListOf(
            Pair(CharacterStat.AVAILABLEAP, remainingAp),
            Pair(CharacterStat.HP, localMaxHp),
            Pair(CharacterStat.MP, localMaxMp),
            Pair(CharacterStat.EXP, exp.get()),
            Pair(CharacterStat.LEVEL, level.toInt()),
            Pair(CharacterStat.MAXHP, maxHp),
            Pair(CharacterStat.MAXMP, maxMp),
            Pair(CharacterStat.STR, str),
            Pair(CharacterStat.DEX, dex)
        )
        if (job.id % 1000 > 0) {
            remainingSp += 3
            statUp.add(Pair(CharacterStat.AVAILABLESP, remainingSp))
        }
        client.announce(CharacterPacket.updatePlayerStats(statUp))
        map.broadcastMessage(this, CharacterPacket.showForeignEffect(id, 0), false)
        recalcLocalStats()
        mpc = PartyCharacter(this)
        silentPartyUpdate()
        if (guildId > 0) {
            getGuild()?.broadcast(GuildPacket.levelUpMessageToGuild(2, level.toInt(), name), id, Guild.BCOp.NONE)
        }
        if (ServerConstants.perfectPitch) {
            if (InventoryManipulator.checkSpace(client, 4310000, 1, "")) {
                InventoryManipulator.addById(client, 4310000, 1)
            }
        }
        if (level.toInt() == 200 && !isGM()) {
            val names = getMedalText() + name
            client.getWorldServer().broadcastPacket(InteractPacket.serverNotice(6, String.format(LEVEL_200 + names)))
        }
        guildUpdate()
    }

    fun logOff() {
        loggedIn = false
    }

    fun message(m: String) = dropMessage(5, m)

    fun yellowMessage(m: String) = announce(PacketCreator.sendYellowTip(m))

    fun mobKilled(id: Int) {
        quests.values.forEach {
            if (it.status == QuestStatus.Status.COMPLETED || it.quest.canComplete(this, null))
                return@forEach
            val progress = it.getProgress(id)
            if (progress.isNotEmpty() && progress.toInt() >= it.quest.getMobAmountNeeded(id))
                return@forEach
            if (it.progress(id))
                client.announce(GameplayPacket.updateQuest(it.quest.id, it.getQuestData()))
        }
    }

    fun mount(id: Int, skillId: Int) {
        mount = Mount(this, id, skillId)
    }

    fun createPlayerNpc(v: Character, scriptId: Int) {
        try {
            transaction {
                if (!(PlayerNpcs.selectAll().where { PlayerNpcs.scriptId eq scriptId }).empty()) {
                    val rs = PlayerNpcs.insert {
                        it[name] = v.name
                        it[hair] = v.hair
                        it[face] = v.face
                        it[skin] = v.skinColor.id
                        it[x] = position.x
                        it[cy] = position.y
                        it[map] = mapId
                        it[PlayerNpcs.scriptId] = scriptId
                        it[foothold] = this@Character.map.footholds?.findBelow(position)?.id ?: 0
                        it[rx0] = position.x + 50
                        it[rx1] = position.x - 50
                    }.resultedValues ?: return@transaction
                    getInventory(InventoryType.EQUIPPED)?.forEach { i ->
                        PlayerNpcsEquip.insert {
                            val position = abs(i.position.toInt())
                            if ((position in 1..11) || (position in 101..111)) {
                                it[npcId] = rs.first()[PlayerNpcs.id]
                                it[equipId] = i.itemId
                                it[equipPos] = i.position.toInt()
                            }
                        }
                    }
                    val row = PlayerNpcs.selectAll().where { PlayerNpcs.scriptId eq scriptId }
                    if (row.empty()) return@transaction
                    val r = row.first()
                    val pn = PlayerNPCs(
                        r[PlayerNpcs.id],
                        r[PlayerNpcs.id],
                        r[PlayerNpcs.name],
                        r[PlayerNpcs.cy],
                        r[PlayerNpcs.hair],
                        r[PlayerNpcs.face],
                        r[PlayerNpcs.skin].toByte(),
                        r[PlayerNpcs.foothold],
                        r[PlayerNpcs.rx0],
                        r[PlayerNpcs.rx1],
                        r[PlayerNpcs.x]
                    )
                    Server.getChannelsFromWorld(world).forEach {
                        val m = it.mapFactory.getMap(mapId)
                        m.broadcastMessage(GameplayPacket.spawnPlayerNpc(pn))
                        m.broadcastMessage(NpcPacket.getPlayerNpc(pn))
                        m.addMapObject(pn)
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save created player npc to database. CharacterId: $id" }
        }
    }

    private fun playerDead() {
        cancelAllBuffs(false)
        dispelDeBuffs()
        eventInstance?.playerKilled(this)
        val charmId = listOf(5130000, 4031283, 4140903)
        var possesed = 0
        var i = 0
        for (element in charmId) {
            val quantity = getItemQuantity(element, false)
            if (possesed == 0 && quantity > 0) {
                possesed = quantity
                break
            }
            i++
        }
        if (possesed > 0) {
            //message("You have used a safety charm, so your EXP points have not been decreased.")
            removeById(client, getInventoryType(charmId[i]), charmId[i], 1, fromDrop = true, consume = false)
        }
        when {
            //mapId in 925020001..925029999 -> dojoStage = 0
            //mapId in 980000101..980000699 -> map.broadcastMessage(this, MiniGamePacket.CPQDied(this))
            job != GameJob.BEGINNER -> {
                var xpDummy = ExpTable.getExpNeededForLevel(level.toInt())
                if (map.town) xpDummy /= 100
                if (xpDummy == ExpTable.getExpNeededForLevel(level.toInt())) {
                    if (luk in 9..100) {
                        xpDummy *= (200 - luk) / 2000
                    } else if (luk < 8) {
                        xpDummy /= 10
                    } else {
                        xpDummy /= 20
                    }
                }
                if (exp.get() > xpDummy) {
                    gainExp(-xpDummy, show = false, inChat = false)
                } else {
                    gainExp(-exp.get(), show = false, inChat = false)
                }
            }
        }
        if (getBuffedValue(BuffStat.MORPH) != null) {
            cancelEffectFromBuffStat(BuffStat.MORPH)
        }
        if (getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
            cancelEffectFromBuffStat(BuffStat.MONSTER_RIDING)
        }
        if (chair == -1) {
            chair = 0
            client.announce(CharacterPacket.cancelChair(-1))
            map.broadcastMessage(this, PacketCreator.showChair(id, 0), false)
        }
        client.announce(PacketCreator.enableActions())
    }

    fun portalDelay(delay: Int) {
        portalDelay = System.currentTimeMillis() + delay.toLong()
    }

    private fun prepareDragonBlood(bloodEffect: StatEffect) {
        dragonBloodSchedule?.cancel()
        dragonBloodSchedule = CoroutineManager.register({
            addHp(-bloodEffect.x)
            client.announce(CharacterPacket.showOwnBuffEffect(bloodEffect.sourceId, 5))
            map.broadcastMessage(this, PacketCreator.showBuffEffect(id, bloodEffect.sourceId, 5), false)
            checkBerserk()
        }, 4000, 4000)
    }

    fun recalcLocalStats() {
        val oldMaxHp = localMaxHp
        localMaxHp = maxHp
        localMaxMp = maxMp
        localDex = dex
        localInt = int
        localStr = str
        localLuk = luk
        magic = localInt
        watk = 0
        getInventory(InventoryType.EQUIPPED)?.forEach {
            it as Equip
            localMaxHp += it.hp
            localMaxMp += it.mp
            localDex += it.dex
            localInt += it.int
            localStr += it.str
            localLuk += it.luk
            magic += it.matk + it.int
            watk += it.watk
            /*speed += it.speed
            jump += it.jump*/
        }
        magic = min(magic, 2000)
        val hbhp = getBuffedValue(BuffStat.HYPERBODYHP)
        if (hbhp != null) {
            localMaxHp += ((hbhp.toDouble() / 100) * localMaxHp).toInt()
        }
        val hbmp = getBuffedValue(BuffStat.HYPERBODYMP)
        if (hbmp != null) {
            localMaxMp += ((hbmp.toDouble() / 100) * localMaxMp).toInt()
        }
        localMaxHp = min(30000, localMaxHp)
        localMaxMp = min(30000, localMaxMp)
        val watkBuff = getBuffedValue(BuffStat.WATK)
        if (watkBuff != null) {
            watk += watkBuff
        }
        if (job.isA(GameJob.BOWMAN)) {
            val expert = if (job.isA(GameJob.MARKSMAN)) SkillFactory.getSkill(3220004)
            else if (job.isA(GameJob.BOWMASTER)) SkillFactory.getSkill(3120005)
            else null
            if (expert != null) {
                val boostLevel = getSkillLevel(expert)
                if (boostLevel > 0) {
                    watk += expert.getEffect(boostLevel.toInt()).x
                }
            }
        }
        val matkBuff = getBuffedValue(BuffStat.MATK)
        if (matkBuff != null) magic += matkBuff
        if (oldMaxHp != 0 && oldMaxHp != localMaxHp) {
            updatePartyMemberHp()
        }
    }

    fun receivePartyMemberHp() {
        if (party != null) {
            val channel = client.channel
            party?.members?.filter { it.mapId == mapId && it.channel == channel }?.forEach {
                val other = it.name?.let { it1 ->
                    Server.getWorld(world).getChannel(channel).players.getCharacterByName(it1)
                }
                if (other != null)
                    client.announce(InteractPacket.updatePartyMemberHp(other.id, other.hp, other.localMaxHp))
            }
        }
    }

    fun registerEffect(effect: StatEffect, startTime: Long, schedule: Job) {
        when {
            effect.isDragonBlood() -> prepareDragonBlood(effect)
            effect.isBerserk() -> checkBerserk()
            effect.isBeholder() -> {
                val beholder = DarkKnight.BEHOLDER
                beholderHealingSchedule?.cancel()
                beholderBuffSchedule?.cancel()
                val healing = SkillFactory.getSkill(DarkKnight.AURA_OF_BEHOLDER) ?: return
                val healingLevel = getSkillLevel(healing)
                if (healingLevel > 0) {
                    val healEffect = healing.getEffect(healingLevel.toInt())
                    val healInterval = healEffect.x * 1000
                    beholderHealingSchedule = CoroutineManager.register({
                        addHp(healEffect.hp)
                        client.announce(CharacterPacket.showOwnBuffEffect(beholder, 2))
                        map.broadcastMessage(this, CharacterPacket.summonSkill(id, beholder, 5), true)
                        map.broadcastMessage(this, CharacterPacket.showOwnBuffEffect(beholder, 2), false)
                    }, healInterval.toLong(), healInterval.toLong())
                }
                val buff = SkillFactory.getSkill(DarkKnight.HEX_OF_BEHOLDER) ?: return
                if (getSkillLevel(buff) > 0) {
                    val buffEffect = buff.getEffect(getSkillLevel(buff).toInt())
                    val buffInterval = buffEffect.x * 1000
                    beholderBuffSchedule = CoroutineManager.register({
                        buffEffect.applyTo(this)
                        client.announce(CharacterPacket.showOwnBuffEffect(beholder, 2))
                        map.broadcastMessage(
                            this, CharacterPacket.summonSkill(
                                id, beholder,
                                ((Math.random() * 3) + 6).toInt()
                            ), true
                        )
                        map.broadcastMessage(this, PacketCreator.showBuffEffect(id, beholder, 2), false)
                    }, buffInterval.toLong(), buffInterval.toLong())
                }
            }

            effect.isRecovery() -> {
                val heal = effect.x
                recoveryTask = CoroutineManager.register({
                    addHp(heal)
                    client.announce(CharacterPacket.showOwnRecovery(heal.toByte()))
                    map.broadcastMessage(this, CharacterPacket.showRecovery(id, heal.toByte()), false)
                }, 5000, 5000)
            }

            else -> {}
        }
        effect.statUps?.forEach {
            effects[it.first] = BuffStatValueHolder(effect, startTime, schedule, it.second)
        }
    }

    fun removeAllCoolDownsExcept(id: Int) {
        coolDowns.values.forEach {
            if (it.skillId != id) coolDowns.remove(it.skillId)
        }
    }

    fun removeCoolDown(skillId: Int) = coolDowns.remove(skillId)

    fun removeVisibleMapObject(mo: MapObject) = visibleMapObjects.remove(mo)

    fun resetStats() {
        var tap = 0
        var tsp = 1
        var tstr = 4
        var tdex = 4
        var tint = 4
        val tluk = 4
        val levelAp = 5
        when (job.id) {
            100 -> {
                tstr = 35
                tap = (level - 10) * levelAp + 14
                tsp += (level - 10) * 3
            }

            200 -> {
                tint = 20
                tap = (level - 8) * levelAp + 29
                tsp += (level - 8) * 3
            }

            300, 400 -> {
                tdex = 25
                tap = (level - 10) * levelAp + 24
                tsp += level - 10 * 3
            }

            500 -> {
                tdex = 20
                tap = (level - 10) * levelAp + 29
                tsp = (level - 10) * 3
            }

            else -> {}
        }
        remainingAp = tap
        remainingSp = tsp
        dex = tdex
        int = tint
        str = tstr
        luk = tluk
        val statUp = listOf(
            Pair(CharacterStat.AVAILABLESP, tsp),
            Pair(CharacterStat.AVAILABLEAP, tap),
            Pair(CharacterStat.STR, tstr),
            Pair(CharacterStat.DEX, tdex),
            Pair(CharacterStat.INT, tint),
            Pair(CharacterStat.LUK, tluk)
        )
        announce(CharacterPacket.updatePlayerStats(statUp))
    }

    fun resetBattleshipHp() {
        battleshipHp =
            4000 * (SkillFactory.getSkill(Corsair.BATTLE_SHIP)?.let { getSkillLevel(it) } ?: 0) + ((level - 120) * 2000)
    }

    fun saveCoolDowns() {
        if (getAllCoolDowns().isNotEmpty()) {
            try {
                transaction {
                    CoolDowns.deleteWhere { CoolDowns.charId eq this@Character.id }
                    getAllCoolDowns().forEach { cd ->
                        CoolDowns.insert {
                            it[charId] = this@Character.id
                            it[skillId] = cd.skillId
                            it[length] = cd.length
                        }
                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to save cool downs to database. CharacterId: $id" }
            }
        }
    }

    fun saveGuildStatus() {
        try {
            transaction {
                Characters.update({ Characters.id eq this@Character.id }) {
                    it[guildId] = this@Character.guildId
                    it[guildRank] = this@Character.guildRank
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save guild status in database. CharacterId: id" }
        }
    }

    fun insertNewChar(): Boolean {
        var success = false
        try {
            var id: Int
            transaction {
                val row = Characters.insert {
                    it[str] = this@Character.str
                    it[dex] = this@Character.dex
                    it[luk] = this@Character.luk
                    it[int] = this@Character.int
                    it[gm] = this@Character.gmLevel.toByte()
                    it[skinColor] = this@Character.skinColor.id
                    it[gender] = this@Character.gender
                    it[job] = this@Character.job.id
                    it[hair] = this@Character.hair
                    it[face] = this@Character.face
                    it[map] = mapId
                    it[meso] = abs(this@Character.meso.get())
                    it[spawnPoint] = 0
                    it[accountId] = this@Character.accountId
                    it[name] = this@Character.name
                    it[world] = this@Character.world
                }.resultedValues
                if (row?.isEmpty() == true) {
                    return@transaction
                }
                id = row?.first()?.get(Characters.id) ?: return@transaction
                for (i in DEFAULT_KEY.indices) {
                    KeyMap.insert {
                        it[characterId] = id
                        it[key] = DEFAULT_KEY[i]
                        it[type] = DEFAULT_TYPE[i]
                        it[action] = DEFAULT_ACTION[i]
                    }
                }
                success = true
                val itemsWithType = mutableListOf<Pair<Item, InventoryType>>()
                inventories.forEach { inv ->
                    inv?.list()?.forEach { item ->
                        itemsWithType.add(Pair(item, inv.type))
                    }
                }
                ItemFactory.INVENTORY.saveItems(itemsWithType, id)
                this@Character.id = id
            }
            return success
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save character to database. CharacterId: $id" }
        }
        return false
    }

    private fun saveKeyValues() {
        try {
            transaction {
                KeyValues.deleteWhere { KeyValues.cid eq this@Character.id }
                customValues.forEach { (t, u) ->
                    KeyValues.insert {
                        it[cid] = this@Character.id
                        it[key] = t
                        it[value] = u
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save custom key value data to database. CharacterId: $id" }
        }
    }

    fun saveLocation(type: String) {
        val closestPortal = map.findClosestPortal(position)
        savedLocations[SavedLocationType.valueOf(type).ordinal] = SavedLocation(mapId, closestPortal?.id ?: 0)
    }

    fun saveToDatabase() {
        try {
            transaction {
                Characters.update({ Characters.id eq this@Character.id }) {
                    it[level] = (if (gmLevel < 1 && this@Character.level > 199) 200 else this@Character.level).toInt()
                    it[fame] = this@Character.fame
                    it[exp] = this@Character.exp.get()
                    it[gachaExp] = this@Character.gachaExp.get()
                    it[hp] = this@Character.hp
                    it[mp] = this@Character.mp
                    it[maxHp] = this@Character.maxHp
                    it[maxMp] = this@Character.maxMp
                    it[ap] = this@Character.remainingAp
                    it[sp] = this@Character.remainingSp
                    it[str] = this@Character.str
                    it[dex] = this@Character.dex
                    it[luk] = this@Character.luk
                    it[int] = this@Character.int
                    it[gm] = this@Character.gmLevel.toByte()
                    it[skinColor] = this@Character.skinColor.id
                    it[gender] = this@Character.gender
                    it[job] = this@Character.job.id
                    it[hair] = this@Character.hair
                    it[face] = this@Character.face
                    it[map] = if (cashShop != null && cashShop?.opened == true) mapId else {
                        if (this@Character.map.forcedReturnMap != 999999999) {
                            this@Character.map.forcedReturnMap
                        } else {
                            if (this@Character.hp < 1) this@Character.map.returnMapId else this@Character.map.mapId
                        }
                    }
                    it[meso] = abs(this@Character.meso.get())
                    it[hpMpUsed] = hpMpApUsed
                    it[spawnPoint] =
                        if (this@Character.map.mapId == 610020000 || this@Character.map.mapId == 610020001) 0 else {
                            this@Character.map.findClosestSpawnPoint(position)?.id ?: 0
                        }
                    it[party] = this@Character.party?.id ?: -1
                    it[buddyCapacity] = buddyList.capacity
                    it[messengerId] = 0
                    it[messengerPosition] = 0
                    it[mountLevel] = mount?.level ?: 1
                    it[mountExp] = mount?.exp ?: 0
                    it[mountTiredness] = mount?.tiredness ?: 0
                    it[equipSlots] = this@Character.getSlots(1)?.toInt() ?: 24
                    it[etcSlots] = this@Character.getSlots(4)?.toInt() ?: 24
                    it[useSlots] = this@Character.getSlots(2)?.toInt() ?: 24
                    it[setupSlots] = this@Character.getSlots(3)?.toInt() ?: 24
                    monsterBook.saveCards(this@Character.id)
                    it[monsterBookCover] = this@Character.bookCover
                    it[matchCardLossess] = matchCardLosses
                    it[matchCardTies] = this@Character.matchCardTies
                    it[matchCardWins] = this@Character.matchCardWins
                    it[omokWins] = this@Character.omokWins
                    it[omokTies] = this@Character.omokTies
                    it[omokLossess] = this@Character.omokLooses
                    it[petHp] = this@Character.petAutoHp
                    it[petMp] = this@Character.petAutoMp
                }
                pet?.saveToDatabase()
                KeyMap.deleteWhere { KeyMap.characterId eq this@Character.id }
                keymap.forEach { (t, u) ->
                    KeyMap.insert {
                        it[characterId] = this@Character.id
                        it[key] = t
                        it[type] = u.type
                        it[action] = u.action
                    }
                }
                val itemsWIthType = mutableListOf<Pair<Item, InventoryType>>()
                inventories.forEach {
                    it?.list()?.forEach { item ->
                        itemsWIthType.add(Pair(item, it.type))
                    }
                }
                ItemFactory.INVENTORY.saveItems(itemsWIthType, this@Character.id)
                Skills.deleteWhere { Skills.characterId eq this@Character.id }
                skills.forEach { (t, u) ->
                    Skills.insert {
                        it[characterId] = this@Character.id
                        it[skillId] = t.id
                        it[skillLevel] = u.skillLevel.toInt()
                        it[masterLevel] = u.masterLevel
                        it[expiration] = u.expiration
                    }
                }
                SavedLocations.deleteWhere { SavedLocations.characterId eq this@Character.id }
                SavedLocationType.values().forEach { t ->
                    val type = savedLocations[t.ordinal] ?: return@forEach
                    SavedLocations.insert {
                        it[characterId] = this@Character.id
                        it[locationType] = t.name
                        it[map] = type.mapId
                        it[portal] = type.portal
                    }
                }
                TrockLocations.deleteWhere { TrockLocations.characterId eq this@Character.id }
                trockMaps.forEach { t ->
                    if (t != 999999999) {
                        TrockLocations.insert {
                            it[characterId] = this@Character.id
                            it[mapId] = t
                            it[vip] = 0
                        }
                    }
                }
                vipTrockMaps.forEach { t ->
                    if (t != 999999999) {
                        TrockLocations.insert {
                            it[characterId] = this@Character.id
                            it[mapId] = t
                            it[vip] = 1
                        }
                    }
                }
                Buddies.deleteWhere { (Buddies.characterId eq this@Character.id) and (Buddies.pending eq 0) }
                buddyList.buddies.values.forEach { e ->
                    Buddies.insert {
                        it[characterId] = this@Character.id
                        it[buddyId] = e.characterId
                        it[pending] = 0
                        it[group] = e.group
                    }
                }
                AreaInfos.deleteWhere { AreaInfos.charId eq this@Character.id }
                areaInfos.forEach { a ->
                    AreaInfos.insert {
                        it[charId] = this@Character.id
                        it[area] = a.key.toInt()
                        it[info] = a.value
                    }
                }
                EventStats.deleteWhere { EventStats.characterId eq this@Character.id }
                QuestStatuses.deleteWhere { QuestStatuses.characterId eq this@Character.id }
                quests.values.forEach { qs ->
                    val qsRs = QuestStatuses.insert {
                        it[characterId] = this@Character.id
                        it[quest] = qs.quest.id.toInt()
                        it[status] = qs.status.id
                        it[time] = ((qs.completionTime / 1000).toInt())
                        it[forfeited] = qs.forfeited
                    }
                    val statusId = qsRs.resultedValues?.first()?.get(QuestStatuses.id) ?: return@forEach
                    qs.progresses.keys.forEach { pk ->
                        QuestProgress.insert {
                            it[questStatusId] = statusId
                            it[progressId] = pk
                            it[progress] = qs.getProgress(pk)
                        }
                    }
                    /*qs.medalMaps.forEach { mm ->
                        MedalMaps.insert {
                            it[questStatusId] = statusId
                            it[value] = mm
                        }
                    }*/
                }
                Accounts.update({ Accounts.id eq client.accountId }) {
                    it[gm] = gmLevel
                }
                cashShop?.save()
                storage?.saveToDatabase()
                if (keyValueChanged) {
                    setKeyValue("HeadTitle", headTitle.toString())
                    saveKeyValues()
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save character to database. CharacterId: $id" }
        }
    }

    fun sendKeymap() = client.announce(PacketCreator.getKeyMap(keymap))

    fun sendNote(to: String, message: String, fame: Byte) {
        try {
            transaction {
                Notes.insert {
                    it[Notes.to] = to
                    it[from] = name
                    it[Notes.message] = message
                    it[timestamp] = Instant.now()
                    it[Notes.fame] = fame.toInt()
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save note data to database. CharacterId: $id, ReceiverName: $to" }
        }
    }

    fun setBuffedValue(effect: BuffStat, value: Int) {
        effects[effect]?.value = value
    }

    fun setRates() {
        val cal = Calendar.getInstance()
        cal.timeZone = TimeZone.getTimeZone("GMT+9")
        val world = Server.getWorld(world)
        val hr = cal[Calendar.HOUR_OF_DAY]
        if (haveItem(5360001) && hr > 6 && hr < 12 || haveItem(5360002) && hr > 9 && hr < 15 || haveItem(536000) && hr > 12 && hr < 18 || haveItem(
                5360004
            ) && hr > 15 && hr < 21 || haveItem(536000) && hr > 18 || haveItem(5360006) && hr < 5 || haveItem(5360007) && hr > 2 && hr < 6 || haveItem(
                5360008
            ) && hr >= 6 && hr < 11
        ) {
            dropRate = 2 * world.dropRate
            mesoRate = 2 * world.mesoRate
        } else {
            dropRate = world.dropRate
            mesoRate = world.mesoRate
        }
        expRate =
            if (haveItem(5211000) && hr > 17 && hr < 21 || haveItem(5211014) && hr > 6 && hr < 12 || haveItem(5211015) && hr > 9 && hr < 15 || haveItem(
                    5211016
                ) && hr > 12 && hr < 18 || haveItem(5211017) && hr > 15 && hr < 21 || haveItem(5211018) && hr > 14 || haveItem(
                    5211039
                ) && hr < 5 || haveItem(5211042) && hr > 2 && hr < 8 || haveItem(5211045) && hr > 5 && hr < 11 || haveItem(
                    5211048
                )
            ) {
                2 * world.expRate
                //if (isBeginnerJob()) 2 else 2 * world.expRate
            } else {
                world.expRate
                //if (isBeginnerJob()) 2 else world.expRate
            }
    }

    fun setHpNormal(value: Int) {
        val oldHp = hp
        var thp = value
        if (thp < 0) thp = 0
        if (thp > localMaxHp) thp = localMaxHp
        hp = thp
        //updatePartyMemberHp()
        if (oldHp > hp && !isAlive()) playerDead()
    }


    fun setHpSilent(delta: Int) {
        hp = delta
        updatePartyMemberHp()
    }

    fun setMpNormal(value: Int) {
        var v = if (value < 0) 0 else value
        v = if (v > localMaxMp) localMaxMp else v
        mp = v
    }

    fun setHpMp(x: Int) {
        setHpNormal(x)
        setMpNormal(x)
        updateSingleStat(CharacterStat.HP, hp)
        updateSingleStat(CharacterStat.MP, mp)
    }

    fun setInventory(type: InventoryType, inv: Inventory) = inventories.set(type.ordinal, inv)

    fun setKeyValue(key: String, values: String) {
        customValues.remove(key)
        customValues[key] = values
        keyValueChanged = true
    }

    fun setMiniGamePoints(visitor: Character, winnerSlot: Int, omok: Boolean) {
        if (omok) {
            when (winnerSlot) {
                1 -> {
                    omokWins++
                    visitor.omokLooses++
                }

                2 -> {
                    visitor.omokWins++
                    omokLooses++
                }

                else -> {
                    omokTies++
                    visitor.omokTies++
                }
            }
        } else {
            when (winnerSlot) {
                1 -> {
                    matchCardWins++
                    visitor.matchCardLosses++
                }

                2 -> {
                    visitor.matchCardWins++
                    matchCardLosses++
                }

                else -> {
                    matchCardTies++
                    visitor.matchCardTies++
                }
            }
        }
    }

    fun showNote() {
        try {
            val list = mutableListOf<Note>()
            transaction {
                Notes.selectAll().where {
                    (Notes.to eq name) and (Notes.deleted eq 0)
                }.forEach {
                    list.add(
                        Note(
                            id = it[Notes.id], from = it[Notes.from], message = it[Notes.message],
                            timestamp = it[Notes.timestamp].toEpochMilli(), fame = it[Notes.fame].toByte()
                        )
                    )
                }
            }
            client.announce(PacketCreator.showNotes(list.toList()))
        } catch (e: SQLException) {
            logger.error(e) { "Failed to get notes data from database. CharacterId: $id" }
        }
    }

    private fun silentEnforceMaxHpMp() {
        mp = mp
        setHpSilent(hp)
    }

    fun silentGiveBuffs(buffs: List<PlayerBuffValueHolder>) {
        buffs.forEach { it.effect.silentApplyBuff(this, it.startTime) }
    }

    fun silentPartyUpdate() = party?.let {
        mpc?.let { it1 ->
            Server.getWorld(world).updateParty(it.id, PartyOperation.SILENT_UPDATE, it1)
        }
    }

    fun skillIsCooling(skillId: Int) = coolDowns.containsKey(skillId)

    fun startFullnessSchedule(decrease: Int, pet: Pet) {
        val schedule = CoroutineManager.register({
            val newFullness = pet.fullness - decrease
            if (newFullness <= 5) {
                pet.fullness = 15
                pet.saveToDatabase()
                unEquipPet()
            } else {
                pet.fullness = newFullness
                pet.saveToDatabase()
                val pet1 = getInventory(InventoryType.CASH)?.getItem(pet.position)
                pet1?.let { forceUpdateItem(it) }
            }
        }, 240000, 1000000)
        fullnessSchedule = schedule
    }

    fun startMapEffect(message: String, itemId: Int, duration: Int = 30000) {
        val mapEffect = MapEffect(message, itemId)
        client.announce(mapEffect.makeStartData())
        CoroutineManager.schedule({
            client.announce(mapEffect.makeDestroyData())
        }, duration.toLong())
    }

    fun timeLimit(second: Int) {
        client.announce(PacketCreator.getClock(second))
        timeSet = second
        clockDelay(second)
    }

    fun toggleHidden() {
        hide(!hidden)
    }

    private fun clockDelay(time: Int) {
        CoroutineManager.schedule({
            timeSet -= 1
            if (timeSet <= 0 && timeSet != -1) {
                val frm = client.getChannelServer().mapFactory.getMap(193000000)
                changeMap(frm)
            } else {
                if (timeSet != -1) clockDelay(timeSet)
            }
        }, 1000L)
    }

    fun stopControllingMonster(monster: Monster) = controlledMonsters.remove(monster)

    fun unEquipPet(hunger: Boolean = false) {
        pet?.summoned = false
        pet?.saveToDatabase()
        fullnessSchedule?.cancel()
        map.broadcastMessage(this, CashPacket.showPet(this, pet, true, hunger), true)
        client.announce(CashPacket.petStatUpdate(this))
        client.announce(PacketCreator.enableActions())
        //TODO: removePet()
    }

    fun unEquipPendantOfSpirit() {
        pendantOfSpirit?.cancel()
        pendantOfSpirit = null
        pendantExp = 0
    }

    fun updatePartyMemberHp() {
        party?.members?.filter { it.mapId == mapId && it.channel == client.channel }?.forEach {
            val other = it.name?.let { it1 ->
                Server.getWorld(world).getChannel(client.channel).players.getCharacterByName(it1)
            }
            other?.client?.announce(InteractPacket.updatePartyMemberHp(id, hp, maxHp))
        }
    }

    fun updateQuest(quest: QuestStatus) {
        quests[quest.quest] = quest
        when (quest.status) {
            QuestStatus.Status.STARTED -> {
                announce(GameplayPacket.questProgress(quest.quest.id, quest.getProgress(0)))
                quest.quest.infoNumber.let {
                    if (it.toInt() > 0) {
                        announce(GameplayPacket.questProgress(it, quest.medalProgress.toString()))
                    }
                }
            }

            QuestStatus.Status.COMPLETED -> {
                announce(GameplayPacket.completeQuest(quest.quest.id, quest.completionTime))
            }

            QuestStatus.Status.NOT_STARTED -> {
                announce(GameplayPacket.forfeitQuest(quest.quest.id))
            }

            else -> return
        }
    }

    fun questTimeLimit(quest: Quest, time: Int) {
        val sf = CoroutineManager.schedule({
            announce(GameplayPacket.questExpire(quest.id))
            val newStatus = QuestStatus(quest, QuestStatus.Status.NOT_STARTED)
            newStatus.forfeited = getQuest(quest).forfeited + 1
            updateQuest(newStatus)
        }, time.toLong())
        announce(GameplayPacket.addQuestTimeLimit(quest.id, time))
        if (sf != null) {
            timers.add(sf)
        }
    }

    fun updateSingleStat(stat: CharacterStat, newVal: Int) {
        announce(CharacterPacket.updatePlayerStats(listOf(Pair(stat, newVal)), itemReaction = false))
    }

    fun updateSingleStat(stat: CharacterStat, newVal: Int, itemReaction: Boolean = false) {
        announce(CharacterPacket.updatePlayerStats(listOf(Pair(stat, newVal)), itemReaction))
    }

    fun dispelDeBuff(deBuff: Disease) {
        if (hasDisease(deBuff)) {
            if (deBuff == Disease.CURSE) client.player?.curse = false
            val mask = deBuff.value
            announce(CharacterPacket.cancelDeBuff(mask))
            map.broadcastMessage(this, CharacterPacket.cancelForeignDeBuff(id, mask), false)
            allDiseases.remove(deBuff)
            diseases.remove(deBuff)
        }
    }

    fun dispelDeBuffs() {
        dispelDeBuff(Disease.CURSE)
        dispelDeBuff(Disease.DARKNESS)
        dispelDeBuff(Disease.POISON)
        dispelDeBuff(Disease.SEAL)
        dispelDeBuff(Disease.WEAKEN)
    }

    fun cancelAllDeBuffs() = diseases.clear()

    private fun hasDisease(dis: Disease): Boolean = diseases.containsKey(dis)

    fun startMobHackParser() {
        mobHackParser?.cancel()
        mobHackParser = CoroutineManager.register({
            checkMobPosition()
        }, 1500L, 6000L)
    }

    fun hide(hide: Boolean, login: Boolean = false) {
        if (isGM() && hide != hidden) {
            if (!hidden) {
                hidden = false
                announce(PacketCreator.getGMEffect(0x10, 0))
                map.broadcastMessage(this, GameplayPacket.spawnPlayerMapObject(this), false)
            } else {
                hidden = true
                announce(PacketCreator.getGMEffect(0x10, 1))
                if (!login) {
                    map.broadcastMessage(this, GameplayPacket.removePlayerFromMap(id), false)
                }
            }
            announce(PacketCreator.enableActions())
        }
    }

    fun newClient(c: Client) {
        loggedIn = true
        c.accountName = client.accountName
        client = c
        val portal = map.findClosestSpawnPoint(position) ?: (map.getPortal(0) ?: return)
        position = portal.position ?: return
        initialSpawnPoint = portal.id
        map = c.getChannelServer().mapFactory.getMap(map.mapId)
    }

    fun getInventory(type: InventoryType) = inventories[type.ordinal]

    private fun getMedalText(): String {
        val medalItem = getInventory(InventoryType.EQUIPPED)?.getItem(-49)
        return medalItem?.let { "<${ItemInformationProvider.getName(it.itemId)}>" } ?: ""
    }

    fun isGM() = gmLevel > 0

    fun blockPortal(scriptName: String) {
        if (!blockedPortals.contains(scriptName)) {
            blockedPortals.add(scriptName)
            client.announce(PacketCreator.enableActions())
        }
    }

    fun unblockPortal(scriptName: String) {
        if (!blockedPortals.contains(scriptName))
            blockedPortals.remove(scriptName)
    }

    fun containsAreaInfo(area: Int, info: String) = areaInfos[area.toShort()]?.contains(info) ?: false

    fun updateAreaInfo(area: Int, info: String) {
        areaInfos[area.toShort()] = info
        announce(PacketCreator.updateAreaInfo(area, info))
    }

    fun getAreaInfo(area: Int) = areaInfos[area.toShort()]

    override val objectType = MapObjectType.PLAYER

    override fun sendDestroyData(client: Client) {
        client.announce(GameplayPacket.removePlayerFromMap(objectId))
    }

    override fun sendSpawnData(client: Client) {
        if (!hidden || (client.player?.gmLevel ?: 0) > 0) {
            client.announce(GameplayPacket.spawnPlayerMapObject(this))
        }
    }

    override var objectId = id
        get() {
            return if (field == 0) id else field
        }
        set(value) {
            field = value
        }

    enum class FameStats {
        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    companion object : KLogging() {
        const val LEVEL_200 = "[축하] %s님이 레벨 200을 달성했습니다. 모두 축하해 주세요."
        val DEFAULT_KEY = arrayOf(
            18,
            65,
            2,
            23,
            3,
            4,
            5,
            6,
            16,
            17,
            19,
            25,
            26,
            27,
            31,
            34,
            35,
            37,
            38,
            40,
            43,
            44,
            45,
            46,
            50,
            56,
            59,
            60,
            61,
            62,
            63,
            64,
            57,
            48,
            29,
            7,
            24,
            33,
            41,
            39
        )
        val DEFAULT_TYPE = arrayOf(
            4,
            6,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            4,
            5,
            5,
            4,
            4,
            5,
            6,
            6,
            6,
            6,
            6,
            6,
            5,
            4,
            5,
            4,
            4,
            4,
            4,
            4
        )
        val DEFAULT_ACTION = arrayOf(
            0,
            106,
            10,
            1,
            12,
            13,
            18,
            24,
            8,
            5,
            4,
            19,
            14,
            15,
            2,
            17,
            11,
            3,
            20,
            16,
            9,
            50,
            51,
            6,
            7,
            53,
            100,
            101,
            102,
            103,
            104,
            105,
            54,
            22,
            52,
            21,
            25,
            26,
            23,
            27
        )

        fun ban(id: String, reason: String, accountId: Boolean): Boolean {
            var result = false
            try {
                transaction {

                    if (id.matches(Regex("/[0-9]{1,3}\\..*"))) {
                        IPBans.insert {
                            it[ip] = id
                        }
                        result = true
                        return@transaction
                    }
                    val rs = if (accountId) {
                        val r = Accounts.select(Accounts.id).where { Accounts.name eq id }
                        if (r.empty()) -1 else r.first()[Accounts.id]
                    } else {
                        val r = Characters.select(Characters.accountId).where { Characters.name eq id }
                        if (r.empty()) -1 else r.first()[Characters.id]
                    }
                    if (rs == -1) return@transaction
                    Accounts.update({ Accounts.id eq rs }) {
                        it[banned] = true
                        it[banReason] = reason
                    }
                    result = true
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to update ban information to database. CharacterId: $id" }
            }
            return result
        }

        fun checkNameAvailable(name: String) = name.toByteArray().size in 3..13 || getIdByName(name) != -1

        fun getCharacterFromDatabase(name: String): CashOperationHandler.Companion.SimpleCharacterInfo? {
            var character: CashOperationHandler.Companion.SimpleCharacterInfo? = null
            try {
                transaction {
                    Characters.select(Characters.id, Characters.accountId, Characters.name).where {
                        Characters.name eq name
                    }.forEach { c ->
                        character = CashOperationHandler.Companion.SimpleCharacterInfo(
                            c[Characters.id],
                            c[Characters.accountId],
                            c[Characters.name]
                        )

                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Error caused when get character data from database using name. Query: $name" }
            }
            return character
        }

        fun getDefault(c: Client): Character {
            val ret = Character(c.world, c.accountId, map = c.getChannelServer().mapFactory.getMap(0), c)
            ret.getInventory(InventoryType.EQUIP)?.slotLimit = 24
            ret.getInventory(InventoryType.USE)?.slotLimit = 24
            ret.getInventory(InventoryType.SETUP)?.slotLimit = 24
            ret.getInventory(InventoryType.ETC)?.slotLimit = 24
            for (i in DEFAULT_KEY.indices) {
                ret.keymap[DEFAULT_KEY[i]] =
                    KeyBinding(DEFAULT_TYPE[i], DEFAULT_ACTION[i])
            }
            if (ret.isGM()) {
                ret.job = GameJob.SUPERGM
                ret.level = 200
            }
            return ret
        }

        fun getIdByName(name: String): Int {
            try {
                var code: Int = -1
                transaction {
                    if (!Characters.select(Characters.id).where { Characters.name eq name }.empty()) {
                        code = 1
                    }
                }
                return code
            } catch (e: SQLException) {
                logger.warn(e) { "Error caused when find character id by name. Query: $name" }
            }
            return -1
        }

        fun loadCharFromDatabase(charId: Int, client: Client, channelServer: Boolean): Character? {
            try {
                var char: Character? = null
                transaction {
                    val rs = Characters.selectAll().where { Characters.id eq charId }
                    if (rs.empty()) return@transaction
                    val chr = rs.first()
                    val map = client.getChannelServer().mapFactory.getMap(0)
                    val ret = Character(
                        world = chr[Characters.world],
                        accountId = chr[Characters.accountId],
                        map = map,
                        client = client
                    )
                    with(ret) {
                        id = charId
                        name = chr[Characters.name]
                        level = chr[Characters.level].toShort()
                        fame = chr[Characters.fame]
                        str = chr[Characters.str]
                        dex = chr[Characters.dex]
                        int = chr[Characters.int]
                        luk = chr[Characters.luk]
                        exp = AtomicInteger(chr[Characters.exp])
                        gachaExp = AtomicInteger(chr[Characters.gachaExp])
                        localMaxHp = chr[Characters.maxHp]
                        hp = chr[Characters.hp]
                        maxHp = chr[Characters.maxHp]
                        mp = chr[Characters.mp]
                        maxMp = chr[Characters.maxMp]
                        hpMpApUsed = chr[Characters.hpMpUsed]
                        hasMerchant = chr[Characters.hasMerchant]
                        remainingSp = chr[Characters.sp]
                        remainingAp = chr[Characters.ap]
                        meso = AtomicInteger(chr[Characters.meso])
                        merchantMeso = chr[Characters.merchantMesos]
                        gmLevel = chr[Characters.gm].toInt()
                        skinColor = SkinColor.getById(chr[Characters.skinColor]) ?: SkinColor.NORMAL
                        gender = chr[Characters.gender]
                        job = GameJob.getById(chr[Characters.job])!!
                        //finishedDojoTutorial = rs.getInt("finishedDojoTutorial") == 1
                        //vanquisherKills = rs.getInt("vanquishKills")
                        omokWins = chr[Characters.omokWins]
                        omokLooses = chr[Characters.omokLossess]
                        omokTies = chr[Characters.omokTies]
                        matchCardWins = chr[Characters.matchCardWins]
                        matchCardLosses = chr[Characters.matchCardLossess]
                        matchCardTies = chr[Characters.matchCardTies]
                        hair = chr[Characters.hair]
                        face = chr[Characters.face]
                        mapId = chr[Characters.map]
                        initialSpawnPoint = chr[Characters.spawnPoint]
                        rank = chr[Characters.rank]
                        rankMove = chr[Characters.rankMove]
                        guildId = chr[Characters.guildId]
                        guildRank = chr[Characters.guildRank]
                        //familyId = rs.getInt("familyId")
                        bookCover = chr[Characters.monsterBookCover]
                        monsterBook = MonsterBook()
                        monsterBook.loadCards(charId)
                        //vanquisherStage = rs.getInt("vanquisherStage")
                        /*dojoPoints = rs.getInt("dojoPoints")
                        dojoStage = rs.getInt("lastDojoStage")*/
                        if (guildId > 0) mgc = GuildCharacter(ret)
                        buddyList = BuddyList(chr[Characters.buddyCapacity])
                        petAutoHp = chr[Characters.petHp]
                        petAutoMp = chr[Characters.petMp]
                        getInventory(InventoryType.EQUIP)?.slotLimit = chr[Characters.equipSlots].toByte()
                        getInventory(InventoryType.USE)?.slotLimit = chr[Characters.useSlots].toByte()
                        getInventory(InventoryType.SETUP)?.slotLimit = chr[Characters.setupSlots].toByte()
                        getInventory(InventoryType.ETC)?.slotLimit = chr[Characters.etcSlots].toByte()
                    }
                    ItemFactory.INVENTORY.loadItems(ret.id, !channelServer).forEach { (item, invType) ->
                        ret.getInventory(invType)?.addFromDatabase(item)
                        if (item.petId > -1) {
                            val pet = item.pet
                            if (pet != null && pet.summoned) {
                                ret.pet = pet
                            }
                            return@forEach
                        }
                        if (invType == InventoryType.EQUIP || invType == InventoryType.EQUIPPED) {
                            item as Equip
                            if (item.ringId > -1) {
                                val ring = Ring.loadFromDatabase(item.ringId) ?: return@forEach
                                if (invType == InventoryType.EQUIPPED) ring.equipped = true
                                //if (item.itemId > 1112012) ret.addFriendshipRing(ring) else ret.addCrushRing(ring)
                            }
                        }
                    }
                    if (channelServer) {
                        val mapFactory = client.getChannelServer().mapFactory
                        ret.map = mapFactory.getMap(ret.mapId)
                        var portal = ret.map.getPortal(ret.initialSpawnPoint)
                        if (portal == null) {
                            portal = ret.map.getPortal(0)
                            ret.initialSpawnPoint = 0
                        }
                        ret.position = portal?.position ?: Point(0, 0)
                        val partyId = chr[Characters.party]
                        val party = Server.getWorld(ret.world).getParty(partyId)
                        if (party != null) {
                            ret.mpc = party.getMemberById(ret.id)
                            ret.mpc?.let { ret.party = party }
                        }
                        ret.loggedIn = true
                    }
                    var v = 0
                    var r = 0
                    TrockLocations.select(TrockLocations.mapId, TrockLocations.vip).where {
                        TrockLocations.characterId eq charId
                    }.limit(15).forEach {
                        val mapId = it[TrockLocations.mapId]
                        if (it[TrockLocations.vip] == 1) {
                            ret.vipTrockMaps[v] = mapId
                            v++
                        } else {
                            ret.trockMaps[v] = mapId
                            r++
                        }
                    }
                    while (v < 10) {
                        ret.vipTrockMaps[v] = 999999999
                        v++
                    }
                    while (r < 5) {
                        ret.trockMaps[r] = 999999999
                        r++
                    }
                    val account = Accounts.select(Accounts.name).where {
                        Accounts.id eq ret.accountId
                    }
                    if (!account.empty()) {
                        ret.client.accountName = account.first()[Accounts.name]
                    }
                    AreaInfos.select(AreaInfos.area, AreaInfos.info).where { AreaInfos.charId eq ret.id }.forEach {
                        ret.areaInfos[it[AreaInfos.area].toShort()] = it[AreaInfos.info]
                    }
                    EventStats.select(EventStats.characterId, EventStats.name)
                        .where { EventStats.characterId eq ret.id }.forEach {
                        val name = it[EventStats.name]
                        if (name == "rescueGaga") {
                            ret.events[name] = RescueGaga(it[EventStats.info])
                        }
                    }
                    ret.cashShop = CashShop(ret.accountId, ret.id)
                    ret.autoban = AutobanManager(ret)
                    val linkedRs = Characters.select(Characters.name, Characters.level).where {
                        (Characters.accountId eq ret.accountId) and (Characters.id neq charId)
                    }.orderBy(Characters.level, order = SortOrder.DESC).limit(1)
                    if (!linkedRs.empty()) {
                        val link = linkedRs.first()
                        ret.linkedName = link[Characters.name]
                        ret.linkedLevel = link[Characters.level]
                    }
                    if (channelServer) {
                        //ret.loadKeyValues()
                        if (ret.getKeyValue("HeadTitle") == null) {
                            ret.setKeyValue("HeadTitle", "0")
                        }
                        ret.headTitle = ret.getKeyValue("HeadTitle")?.toInt() ?: 0
                        QuestStatuses.selectAll().where { QuestStatuses.characterId eq charId }.forEach { s ->
                            val q = Quest.getInstance(s[QuestStatuses.quest])
                            val status = QuestStatus(
                                q,
                                (QuestStatus.Status.getById(s[QuestStatuses.status]) ?: QuestStatus.Status.NOT_STARTED)
                            )
                            val cTime = s[QuestStatuses.time]
                            if (cTime > 1) status.completionTime = (cTime * 1000).toLong()
                            status.forfeited = s[QuestStatuses.forfeited]
                            ret.quests[q] = status
                            QuestProgress.selectAll().where { QuestProgress.questStatusId eq s[QuestStatuses.id] }
                                .forEach { progress ->
                                    status.setProgress(
                                        progress[QuestProgress.progressId],
                                        progress[QuestProgress.progress]
                                    )
                                }
                            /*MedalMaps.slice(MedalMaps.mapId).select { MedalMaps.questStatusId eq s[QuestStatus.id] }.forEach { m ->
                                status.addMedalMap(m[MedalMaps.mapId])
                            }*/
                        }
                        Skills.selectAll().where { Skills.characterId eq charId }.forEach {
                            val skill = SkillFactory.getSkill(it[Skills.skillId]) ?: return@forEach
                            ret.skills[skill] = SkillEntry(
                                it[Skills.skillLevel].toByte(),
                                it[Skills.masterLevel],
                                it[Skills.expiration]
                            )
                        }
                        CoolDowns.selectAll().where { CoolDowns.charId eq ret.id }.forEach {
                            val skillId = it[CoolDowns.skillId]
                            val length = it[CoolDowns.length]
                            val startTime = it[CoolDowns.startTime].toEpochMilli()
                            if (skillId != 5221999 && (length + startTime < System.currentTimeMillis())) {
                                return@forEach
                            }
                            ret.giveCoolDowns(skillId, startTime, length)
                        }
                        KeyMap.selectAll().where { KeyMap.characterId eq charId }.forEach {
                            ret.keymap[it[KeyMap.key]] = KeyBinding(it[KeyMap.type], it[KeyMap.action])
                        }
                        SavedLocations.selectAll().where { SavedLocations.characterId eq charId }.forEach {
                            ret.savedLocations[SavedLocationType.valueOf(it[SavedLocations.locationType]).ordinal] =
                                SavedLocation(
                                    it[SavedLocations.map], it[SavedLocations.portal]
                                )
                        }
                        FameLog.selectAll().where { FameLog.characterId eq charId }
                            .orderBy(FameLog.timestamp, SortOrder.ASC).limit(10).forEach {
                            ret.lastFameTime =
                                max(ret.lastFameTime.toLong(), it[FameLog.timestamp].toEpochMilli()).toInt()
                            ret.lastMonthFameIds.add(it[FameLog.characterIdTo])
                        }
                        ret.buddyList.loadFromDatabase(charId)
                        ret.recalcLocalStats()
                        ret.silentEnforceMaxHpMp()
                    }
                    val mountId = ret.getJobType() * 10000000 + 1004
                    val mountItem = ret.getInventory(InventoryType.EQUIPPED)?.getItem(-18)
                    if (mountItem != null) {
                        ret.mount = Mount(ret, mountItem.itemId, mountId)
                    } else {
                        ret.mount = Mount(ret, 0, mountId)
                    }
                    ret.mount?.exp = chr[Characters.mountExp]
                    ret.mount?.level = chr[Characters.mountLevel]
                    ret.mount?.tiredness = chr[Characters.mountTiredness]
                    ret.mount?.isActive = false
                    char = ret
                }
                return char
            } catch (e: Exception) {
                logger.error(e) { "Failed to load character from database. CharacterId: $charId" }
            }
            return null
        }

        fun makeReadable(r: String): String {
            var i = r.replace('I', 'i')
            i = i.replace('l', 'L')
            i = i.replace("rn", "Rn")
            i = i.replace("vv", "Vv")
            i = i.replace("VV", "Vv")
            return i
        }

        fun rand(left: Int, right: Int) = Random.nextInt((right - left + 1) + left)

        class CancelCoolDownAction(val target: Character, val skillId: Int) : Runnable {
            override fun run() {
                target.removeCoolDown(skillId)
                target.client.announce(CharacterPacket.skillCoolDown(skillId, 0))
            }
        }

        data class BuffStatValueHolder(val effect: StatEffect, val startTime: Long, val schedule: Job, var value: Int)

        data class CoolDownValueHolder(val skillId: Int, val startTime: Long, val length: Long, val timer: Job?)

        data class SkillEntry(val skillLevel: Byte, val masterLevel: Int, val expiration: Long) {
            override fun toString() = "$skillLevel:$masterLevel"
        }

        data class Note(val id: Int, val from: String, val message: String, val timestamp: Long, val fame: Byte)
    }
}