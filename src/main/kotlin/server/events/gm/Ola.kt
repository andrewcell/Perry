package server.events.gm

import client.Character
import tools.CoroutineManager
import tools.PacketCreator
import tools.packet.InteractPacket

class Ola(val chr: Character) {
    var time = 0L
    private var timeStarted = 0L
    val schedule = CoroutineManager.schedule({
        if (chr.mapId in 109030001..109030303) {
            chr.changeMap(chr.map.returnMapId)
            resetTimes()
        }
    }, 360000)

    fun startOla() {
        chr.map.eventStarted = true
        chr.client.announce(PacketCreator.getClock(360))
        timeStarted = System.currentTimeMillis()
        time = 360000
        chr.map.getPortal("join00")?.portalStatus = true
        chr.client.announce(InteractPacket.serverNotice(0, "The portal has now opened. Press the up arrow key at the portal to enter."))
    }

    fun isTimerStarted(): Boolean = time > 0 && timeStarted > 0

    fun resetTimes() {
        time = 0
        timeStarted = 0
        schedule.cancel()
    }

    fun getTimeLeft() = time - (System.currentTimeMillis() - timeStarted)
}