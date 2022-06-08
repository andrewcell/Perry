package server.maps

import client.Client
import mu.KLoggable
import scripting.reactor.ReactorScriptManager
import tools.CoroutineManager
import tools.packet.GameplayPacket
import java.awt.Point
import java.awt.Rectangle

class Reactor(val stats: ReactorStats, val rid: Int, val delay: Int, var state: Byte, val name: String,
              pos: Point) : AbstractMapObject(), KLoggable {
    override val logger = logger()
    var alive = true
    var timerActive = false
    var map: GameMap? = null

    fun getReactItem(index: Int) = stats.getReactItem(state, index)

    fun delayedHitReactor(c: Client, delay: Long) {
        CoroutineManager.schedule({
            hitReactor(c)
        }, delay)
    }

    fun hitReactor(c: Client, charPos: Int = 0, stance: Short = 0, skillId: Int = 0) {
        try {
            if (stats.getType(state) < 999 && stats.getType(state) != -1) {
                if (!(stats.getType(state) == 2 && (charPos == 0 || charPos == 2))) {
                    //get next state
                    for (b in 0 until stats.getStateSize(state)) {
                        val activeSkills = stats.getActiveSkills(state, b)
                        if (activeSkills?.contains(skillId) == false) continue
                        state = stats.getNextState(state, b)
                        if (stats.getNextState(state, b).toInt() == -1) {// End of reactor
                            if (stats.getType(state) < 100) { //Reactor broken
                                if (delay > 0)
                                    map?.destroyReactor(objectId)
                                else
                                    map?.broadcastMessage(GameplayPacket.triggerReactor(this, stance.toInt()))
                            } else { // Item-triggered on final step
                                map?.broadcastMessage(GameplayPacket.triggerReactor(this, stance.toInt()))
                            }
                            ReactorScriptManager.act(c, this)
                        } else { // Reactor not broken yet
                            map?.broadcastMessage(GameplayPacket.triggerReactor(this, stance.toInt()))
                            if (state == stats.getNextState(state, b)) {
                                ReactorScriptManager.act(c, this)
                            }
                        }
                        break
                    }
                }
            } else {
                state++
                map?.broadcastMessage(GameplayPacket.triggerReactor(this, stance.toInt()))
                ReactorScriptManager.act(c, this)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error caused when handle hit reactor." }
        }
    }

    fun getArea() = Rectangle(position.x + stats.tl?.x!!, position.y + stats.tl?.y!!, stats.br?.x!! - stats.tl?.x!!, stats.br?.y!! - stats.tl?.y!!)

    fun getReactorType() = stats.getType(state)

    fun makeSpawnData(): ByteArray = GameplayPacket.spawnReactor(this)

    override val objectType = MapObjectType.REACTOR

    override var position: Point = pos

    override fun sendSpawnData(client: Client) {
        client.announce(makeSpawnData())
    }

    override fun sendDestroyData(client: Client) {
        client.announce(GameplayPacket.spawnReactor(this))
    }
}