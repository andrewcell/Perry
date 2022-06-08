package server.events.gm

import client.Character
import server.maps.GameMap
import tools.CoroutineManager
import tools.PacketCreator
import tools.packet.MiniGamePacket
import java.util.*

class Snowball(private val team: Int, val map: GameMap) {
    val characters = LinkedList<Character>()
    var position = 0
    private var hits = 25
    var snowmanHp = 7500
    private var isHittable = false
    private var winner = false

    init {
        for (chr in map.characters) {
            if (chr.eventTeam == team) {
                characters.add(chr)
            }
        }
    }

    fun startEvent() {
        if (isHittable) return
        for (chr in characters) {
            chr.announce(MiniGamePacket.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)))
            chr.announce(PacketCreator.getClock(600))
        }
        isHittable = true
        CoroutineManager.schedule({
            fun run() {
                val snowball = map.getSnowball(team)
                val snowballOpposite = map.getSnowball(if (team == 0) 1 else 0)
                if (snowball != null && snowballOpposite != null) {
                if (snowball.position > snowballOpposite.position) {
                    for (chr in characters) {
                        chr.announce(MiniGamePacket.rollSnowBall(false, 3, map.getSnowball(0), map.getSnowball(0)))
                    }
                    winner = true
                } else if (snowballOpposite.position > snowball.position) {
                    for (chr in characters) {
                        chr.announce(MiniGamePacket.rollSnowBall(false, 4, map.getSnowball(0), map.getSnowball(0)))
                    }
                    winner = true
                } else {
                    warpOut()
                }
                }
            }
        }, 600000)
    }

    fun hit(what: Int, damage: Int) {
        if (what < 2) {
            if (damage > 0) hits--
        } else {
            if (snowmanHp - damage < 0) {
                snowmanHp = 0
                CoroutineManager.schedule({
                    fun run() {
                        snowmanHp = 7500
                        message(5)
                    }
                }, 10000)
            } else {
                snowmanHp -= damage
                map.broadcastMessage(MiniGamePacket.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)))
            }

            if (hits == 0) {
                position += 1
                val tm = if(team == 0) 1 else 0
                when (position) {
                    45 -> map.getSnowball(tm)?.message(1)
                    290 -> map.getSnowball(tm)?.message(2)
                    560 -> map.getSnowball(tm)?.message(2)
                }
                hits = 25
                map.broadcastMessage(MiniGamePacket.rollSnowBall(false, 0, map.getSnowball(0), map.getSnowball(1)))
                map.broadcastMessage(MiniGamePacket.rollSnowBall(false, 1, map.getSnowball(0), map.getSnowball(1)))
            }
            map.broadcastMessage(MiniGamePacket.hitSnowBall(what, damage))
        }
    }

    fun message(message: Int) {
        for (chr in characters) {
            chr.announce(MiniGamePacket.snowballMessage(team, message))
        }
    }

    private fun warpOut() {
        CoroutineManager.schedule({
            fun run() {
                if (winner) {
                    map.warpOutByTeam(team, 109050000)
                } else {
                    map.warpOutByTeam(team, 109050001)
                }
                map.setSnowball(team, null)
            }
        }, 10000)
    }
}