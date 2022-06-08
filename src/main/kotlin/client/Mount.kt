package client

import kotlinx.coroutines.Job
import tools.CoroutineManager
import tools.packet.CharacterPacket

class Mount(var owner: Character, var itemId: Int, val skillId: Int) {
    var tiredness = 0
        set(value) {
            field = value
            if (field < 0)
                field = 0
        }
    var level = 1
    var exp = 0
    var isActive = true
    private var tirednessSchedule: Job? = null

    /**
     * 1902000 - Hog
     * 1902001 - Silver Mane
     * 1902002 - Red Draco
     * 1902005 - Mimiana
     * 1902006 - Mimio
     * 1902007 - Shinjou
     * 1902008 - Frog
     * 1902009 - Ostrich
     * 1902010 - Frog
     * 1902011 - Turtle
     * 1902012 - Yeti
     * @return the id
     */
    fun getId() = if (itemId < 1903000) itemId - 1901999 else 5

    private fun increaseTiredness() {
        tiredness++
        owner.map.broadcastMessage(CharacterPacket.updateMount(owner.id, this, false))
        if (tiredness > 99) {
            tiredness = 95
            owner.dispelSkill(owner.getJobType() * 10000000 + 1004)
        }
    }

    fun startSchedule() {
        tirednessSchedule = CoroutineManager.register({
            increaseTiredness()
        }, 60000, 60000)
    }

    fun cancelSchedule() = tirednessSchedule?.cancel()

    fun empty() {
        cancelSchedule()
        tirednessSchedule = null
    }
}