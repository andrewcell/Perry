package server.events.gm

import client.Character
import kotlinx.coroutines.Job
import tools.CoroutineManager
import tools.PacketCreator
import tools.packet.InteractPacket

class Fitness(val chr: Character) {
    var time = 0L
    private var timeStarted = 0L
    var schedule: Job? = CoroutineManager.schedule({
        if (chr.mapId in 109040000..109040004) chr.changeMap(chr.map.returnMapId)
    }, 900000)
    private var scheduleMsg: Job? = null

    fun startFitness() {
        chr.map.eventStarted = true
        chr.client.announce(PacketCreator.getClock(900))
        timeStarted = System.currentTimeMillis()
        checkAndMessage()
        chr.map.getPortal("join00")?.portalState = true
        chr.client.announce(InteractPacket.serverNotice(0, "The portal has now opened. Press the up arrow key at the portal to enter."))
    }

    fun isTimerStarted(): Boolean = time > 0 && timeStarted > 0

    fun resetTimes() {
        time = 0
        timeStarted = 0
        schedule?.cancel()
        scheduleMsg?.cancel()
    }

    fun getTimeLeft(): Long = time - (System.currentTimeMillis() - timeStarted)

    private fun checkAndMessage() {
        scheduleMsg = CoroutineManager.register({
            if (chr.fitness == null) resetTimes()
            if (chr.map.mapId in 109040000..109040004) {
                val message = when (getTimeLeft()) {
                    in 9001..10999 -> "You have 10 sec left. Those of you unable to beat the game, we hope you beat it next time! Great job everyone!! See you later~"
                    in 99001..100999 -> "Alright, you don't have much time remaining. Please hurry up a little!"
                    in 239001..240999 -> "The 4th stage is the last one for [The Physical Fitness Test]. Please don't give up at the last minute and try your best. The reward is waiting for you at the very top!"
                    in 299001..300999 -> "The 3rd stage offers traps where you may see them, but you won't be able to step on them. Please be careful of them as you make your way up."
                    in 359001..360999 -> "For those who have heavy lags, please make sure to move slowly to avoid falling all the way down because of lags."
                    in 499001..500999 -> "Please remember that if you die during the event, you'll be eliminated from the game. If you're running out of HP, either take a potion or recover HP first before moving on."
                    in 599001..600999 -> "The most important thing you'll need to know to avoid the bananas thrown by the monkeys is *Timing* Timing is everything in this!"
                    in 659001..660999 -> "The 2nd stage offers monkeys throwing bananas. Please make sure to avoid them by moving along at just the right timing."
                    in 699001..700999 -> "Please remember that if you die during the event, you'll be eliminated from the game. You still have plenty of time left, so either take a potion or recover HP first before moving on."
                    in 779001..780999 -> "Everyone that clears [The Physical Fitness Test] on time will be given an item, regardless of the order of finish, so just relax, take your time, and clear the 4 stages."
                    in 839001..840999 -> "There may be a heavy lag due to many users at stage 1 all at once. It won't be difficult, so please make sure not to fall down because of heavy lag."
                    in 869001..870999 -> "[Physical Fitness Test] consists of 4 stages, and if you happen to die during the game, you'll be eliminated from the game, so please be careful of that."
                    else -> ""
                }
                chr.client.announce(InteractPacket.serverNotice(0, message))
            } else {
                resetTimes()
            }
        }, 5000, 29500)
    }
}
