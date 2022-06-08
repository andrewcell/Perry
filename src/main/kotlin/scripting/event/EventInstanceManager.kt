package scripting.event

import client.Character
import database.EventStats
import mu.KLoggable
import net.server.world.Party
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import provider.DataProviderFactory
import server.life.Monster
import server.maps.GameMap
import server.maps.MapFactory
import tools.CoroutineManager
import tools.ServerJSON
import java.io.File
import java.util.*

class EventInstanceManager(private val em: EventManager, val name: String) : KLoggable {
    override val logger = logger()
    private val chars = mutableListOf<Character>()
    val mobs = mutableListOf<Monster>()
    private val killCount = mutableMapOf<Character, Int>()
    private val props = Properties()
    private var timeStarted = 0L
    private var eventTime = 0L
    val mapFactory = MapFactory(DataProviderFactory.getDataProvider(File("${ServerJSON.settings.wzPath}/Map.wz")),
        DataProviderFactory.getDataProvider(File("${ServerJSON.settings.wzPath}/String.wz")),
        0, 1)

    init {
        mapFactory.channel = em.channelServer.channelId
    }

    fun disbandParty() {
        try {
            em.iv.invokeFunction("disbandParty", this)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to disband party in script. Name: $name" }
        }
    }

    fun dispose() {
        chars.clear()
        mobs.clear()
        killCount.clear()
        em.disposeInstance(name)
        //em = null
    }

    fun finishPQ() {
        try {
            em.iv.invokeFunction("clearPQ", this)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to finish party quest in script. Name: $name" }
        }
    }

    fun isTimerStarted() = eventTime > 0 && timeStarted > 0

    fun getKillCount(chr: Character) = killCount[chr] ?: 0

    fun getMapInstance(mapId: Int): GameMap {
        val map = mapFactory.getMap(mapId)
        if (!mapFactory.isMapLoaded(mapId)) {
            if (em.shuffleReactors)
                map.shuffleReactors()
        }
        return map

    }

    fun getPlayerCount() = chars.size

    fun getPlayers() = chars.toList()

    fun getProperty(key: String): String {
        return props.getProperty(key)
    }

    fun getTimeLeft() = eventTime - (System.currentTimeMillis() - timeStarted)

    fun isLeader(chr: Character) = chr.party?.leader?.id == chr.id

    fun leftParty(chr: Character) {
        try {
            em.iv.invokeFunction("leftParty", this, chr)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to left party in script. Name: $name" }
        }
    }

    fun monsterKilled(chr: Character, mob: Monster) {
        try {
            var kc = killCount[chr]
            val inc = (em.iv.invokeFunction("monsterValue", this, mob.id) as? Double)?.toInt() ?: return
            if (kc == null) {
                kc = inc
            } else {
                kc += inc
            }
            killCount[chr] = kc
        } catch (e: Exception) {
            logger.warn(e) { "Failed to handle monster killed in event script. Name: $name" }
        }
    }

    fun playerDisconnected(chr: Character) {
        try {
            em.iv.invokeFunction("playerDisconnected", this, chr)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to handle player disconnected in event script. Name: $name" }
        }
    }

    fun playerKilled(chr: Character) {
        try {
            em.iv.invokeFunction("playerDead", this, chr)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to handle player killed in event script." }
        }
    }

    fun registerMonster(mob: Monster) {
        mobs.add(mob)
        mob.eventInstance = this
    }

    fun registerParty(party: Party, map: GameMap) {
        party.members.forEach {
            val c = map.getCharacterById(it.id ?: return@forEach)
            c?.let { it1 -> registerPlayer(it1) }
        }
    }

    private fun registerPlayer(chr: Character) {
        try {
            chars.add(chr)
            chr.eventInstance = this
            em.iv.invokeFunction("playerEntry", this, chr)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to handle player entry in event script. Name: $name" }
        }
    }

    fun removePlayer(chr: Character) {
        try {
            em.iv.invokeFunction("playerExit", this, chr)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to handle player exit in event script. Name: $name" }
        }
    }

    fun revivePlayer(chr: Character): Boolean {
        try {
            val b = em.iv.invokeFunction("playerRevive", this, chr)
            if (b is Boolean) return b
        } catch (e: Exception) {
            logger.warn(e) { "Failed to handle revive player in event script. Name: $name" }
        }
        return true
    }

    fun saveWinner(chr: Character) {
        try {
            transaction {
                EventStats.insert {
                    it[name] = em.name
                    it[instance] = name
                    it[characterId] = chr.id
                    it[channel] = chr.client.channel
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to save event stats in event script due to database error. Name: $name" }
        }
    }

    fun setProperty(key: String, value: String): Any = props.setProperty(key, value)

    fun schedule(methodName: String, delay: Long) {
        CoroutineManager.schedule({
            try {
                em.iv.invokeFunction(methodName, this)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to schedule coroutine job in event script. Name: $name, MethodName: $methodName" }
            }
        }, delay)
    }

    fun startEventTimer(time: Long) {
        timeStarted = System.currentTimeMillis()
        eventTime = time
    }

    fun unregisterMonster(mob: Monster) {
        mobs.remove(mob)
        mob.eventInstance = null
        if (mobs.isEmpty()) {
            try {
                em.iv.invokeFunction("allMonstersDead", this)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to handle all monsters dead in event script. Name: $name" }
            }
        }
    }

    fun unregisterPlayer(chr: Character) {
        chars.remove(chr)
        chr.eventInstance = null
    }
}