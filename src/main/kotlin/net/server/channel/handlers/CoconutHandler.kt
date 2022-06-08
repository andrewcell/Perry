package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket
import tools.packet.MiniGamePacket

class CoconutHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        /*CB 00 A6 00 06 01
         * A6 00 = coconut id
         * 06 01 = ?
         */
        val id = slea.readShort().toInt()
        val map = c.player?.map
        val event = map?.coconut
        val nut = event?.getCoconut(id)
        if (nut?.isHittable != true) return
        if (System.currentTimeMillis() < nut.hitTime) return
        if (nut.hits > 2 && Math.random() < 0.4) {
            if (Math.random() < 0.01 && event.countStopped > 0) {
                nut.isHittable = false
                event.stopCoconut()
                map.broadcastMessage(MiniGamePacket.hitCoconut(false, id, 1))
                return
            }
            nut.isHittable = false // for sure :)
            nut.resetHits() // For next event (without restarts)
            if (Math.random() < 0.05 && event.countBombing > 0) {
                map.broadcastMessage(MiniGamePacket.hitCoconut(false, id, 2))
                event.bombCoconut()
            } else if (event.countFalling > 0) {
                map.broadcastMessage(MiniGamePacket.hitCoconut(false, id, 3))
                event.fallCoconut()
                if (c.player?.eventTeam == 0) {
                    event.addMScore()
                    map.broadcastMessage(
                        InteractPacket.serverNotice(
                            5,
                            "${c.player?.name} of Team M. knocks down a coconut."
                        )
                    )
                } else {
                    event.addStoryScore()
                    map.broadcastMessage(
                        InteractPacket.serverNotice(
                            5,
                            "${c.player?.name} of Team Story knocks down a coconut."
                        )
                    )
                }
                map.broadcastMessage(MiniGamePacket.coconutScore(event.mScore, event.storyScore))
            }
        } else {
            nut.hit()
            map.broadcastMessage(MiniGamePacket.hitCoconut(false, id, 1))
        }
    }
}