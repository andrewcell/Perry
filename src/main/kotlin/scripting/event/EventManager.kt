package scripting.event

import kotlinx.coroutines.Job
import mu.KLoggable
import net.server.channel.Channel
import net.server.world.Party
import server.maps.GameMap
import tools.CoroutineManager
import tools.PacketCreator
import java.util.*
import java.util.concurrent.CancellationException
import javax.script.Invocable

class EventManager(val channelServer: Channel, val iv: Invocable, val name: String) : KLoggable {
    override val logger = logger()
    var schedule: Job? = null
    var shuffleReactors = false
    val props = Properties()
    private val instances = mutableMapOf<String, EventInstanceManager>()

    fun cancel() {
        try {
            iv.invokeFunction("cancelSchedule", null)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to cancel schedule in event. Name: $name" }
        }
    }

    fun cancelSchedule() = schedule?.cancel(CancellationException())

    fun schedule(methodName: String, delay: Long) = schedule(methodName, delay, null);

    fun schedule(methodName: String, delay: Long, eim: EventInstanceManager? = null) {
        schedule = CoroutineManager.schedule({
            try {
                iv.invokeFunction(methodName, eim)
            } catch (e: Exception) {
                logger.error(e) { "Error in event method $methodName"}
            }
        }, delay)
    }

    fun scheduleAtTimestamp(methodName: String, timestamp: Long) = CoroutineManager.scheduleAtTimestamp({
            try {
                iv.invokeFunction(methodName, null)
            } catch (e: Exception) {
                logger.error(e) { "Error in event method $methodName"}
            }
        }, timestamp)

    fun getInstance(name: String) = instances[name]

    fun newInstance(name: String): EventInstanceManager {
        val ret = EventInstanceManager(this, name)
        instances[name] = ret
        return ret
    }

    fun disposeInstance(name: String) = instances.remove(name)

    fun startInstance(party: Party, map: GameMap) {
        try {
            val eim = iv.invokeFunction("setup", null) as EventInstanceManager
            eim.registerParty(party, map)
        } catch (e: Exception) {
            logger.error(e) { "Error in event manager. startInstance"}
        }
    }

    fun startInstance(eim: EventInstanceManager, leader: String) {
        try {
            iv.invokeFunction("setup", eim)
            eim.setProperty("leader", leader)
        } catch (e: Exception) {
            logger.error(e) { "Error in event manager. Party leader:$leader startInstance"}
        }
    }

    fun warpAllPlayer(from: Int, to: Int) {
        val toMap = getMapFactory().getMap(to)
        val fromMap = getMapFactory().getMap(from)
        val list = fromMap.getCharactersThreadSafe()
        if (fromMap.getCharactersSize() > 0) {
            list.forEach {
                it.changeMap(toMap, toMap.getPortal(0))
            }
        }
    }

    fun getMapFactory() = channelServer.mapFactory

    fun broadcastShip(mapId: Int, type: Boolean) {
        val map = getMapFactory().getMap(mapId)
        if (map.getCharactersSize() != 0) {
            map.broadcastMessage(PacketCreator.boatPacket(type))
        }
        map.docked = type
    }

    fun setProperty(key: String, value: String) = props.setProperty(key, value)

    fun getProperty(key: String) = props.getProperty(key)
}