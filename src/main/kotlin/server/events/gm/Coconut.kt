
package server.events.gm

import server.maps.GameMap
import tools.CoroutineManager.schedule
import tools.PacketCreator
import tools.packet.GameplayPacket
import tools.packet.MiniGamePacket
import java.util.*


class Coconut(val map: GameMap) : Event(1, 50) {
    var mScore = 0
    var storyScore = 0
    var countBombing = 80
    var countFalling = 401
    var countStopped = 20
    private val coconuts = LinkedList<Coconuts>()

    fun startEvent() {
        map.eventStarted = true
        for (i in 0..505) {
            coconuts.add(Coconuts(i))
        }
        map.broadcastMessage(MiniGamePacket.hitCoconut(true, 0, 0))
        setCoconutsHittable(true)
        map.broadcastMessage(PacketCreator.getClock(300))
        schedule(Runnable {
            if (map.mapId == 109080000) {
                if (mScore == storyScore) {
                    bonusTime()
                } else if (mScore > storyScore) {
                    for (chr in map.characters) {
                        if (chr.eventTeam == 0) {
                            chr.client.announce(GameplayPacket.showEffect("event/coconut/victory"))
                            chr.client.announce(GameplayPacket.playSound("Coconut/Victory"))
                        } else {
                            chr.client.announce(GameplayPacket.showEffect("event/coconut/lose"))
                            chr.client.announce(GameplayPacket.playSound("Coconut/Failed"))
                        }
                    }
                    warpOut()
                } else {
                    for (chr in map.characters) {
                        if (chr.eventTeam == 1) {
                            chr.client.announce(GameplayPacket.showEffect("event/coconut/victory"))
                            chr.client.announce(GameplayPacket.playSound("Coconut/Victory"))
                        } else {
                            chr.client.announce(GameplayPacket.showEffect("event/coconut/lose"))
                            chr.client.announce(GameplayPacket.playSound("Coconut/Failed"))
                        }
                    }
                    warpOut()
                }
            }
        }, 300000)
    }

    private fun bonusTime() {
        map.broadcastMessage(PacketCreator.getClock(120))
        schedule({
            if (mScore == storyScore) {
                for (chr in map.characters) {
                    chr.client.announce(GameplayPacket.showEffect("event/coconut/lose"))
                    chr.client.announce(GameplayPacket.playSound("Coconut/Failed"))
                }
                warpOut()
            } else if (mScore > storyScore) {
                for (chr in map.characters) {
                    if (chr.eventTeam == 0) {
                        chr.client.announce(GameplayPacket.showEffect("event/coconut/victory"))
                        chr.client.announce(GameplayPacket.playSound("Coconut/Victory"))
                    } else {
                        chr.client.announce(GameplayPacket.showEffect("event/coconut/lose"))
                        chr.client.announce(GameplayPacket.playSound("Coconut/Failed"))
                    }
                }
                warpOut()
            } else {
                for (chr in map.characters) {
                    if (chr.eventTeam == 1) {
                        chr.client.announce(GameplayPacket.showEffect("event/coconut/victory"))
                        chr.client.announce(GameplayPacket.playSound("Coconut/Victory"))
                    } else {
                        chr.client.announce(GameplayPacket.showEffect("event/coconut/lose"))
                        chr.client.announce(GameplayPacket.playSound("Coconut/Failed"))
                    }
                }
                warpOut()
            }
        }, 120000)
    }

    private fun warpOut() {
        setCoconutsHittable(false)
        schedule(Runnable {
            for (chr in map.characters) {
                if ((mScore > storyScore && chr.eventTeam == 0) || (storyScore > mScore && chr.eventTeam == 1))
                    chr.changeMap(109050000)
                else
                    chr.changeMap(109050001)
            }
            map.coconut = null
        }, 12000)
    }

    fun addMScore() {
        mScore += 1
    }

    fun addStoryScore() {
        storyScore += 1
    }

    fun fallCoconut() {
        countFalling--
    }


    fun bombCoconut() {
        countBombing--
    }


    fun stopCoconut() {
        countStopped--
    }

    fun getCoconut(id: Int) = coconuts[id]

    private fun setCoconutsHittable(hittable: Boolean) {
        for (nut in coconuts) {
            nut.isHittable = hittable
        }
    }
}