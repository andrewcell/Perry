package server.events

import client.Character
import client.SkillFactory

class RescueGaga(var completed: Int) : Events() {
    private var fallen: Byte = 0

    fun fallAndGet(): Int {
        fallen++
        if (fallen > 3) {
            fallen = 0
            return 4
        }
        return fallen.toInt()
    }

    fun complete() = completed++

    fun giveSkill(chr: Character) {
        var skillId = 0
        when (chr.getJobType()) {
            0 -> skillId = 1013
            1, 2 -> skillId = 10001014
        }
        val expiration = (System.currentTimeMillis() + (3600 * 24 * 20 * 1000)) // 20 days
        if (completed < 20) {
            SkillFactory.getSkill(skillId)?.let { chr.changeSkillLevel(it, 1, 1, expiration) }
            SkillFactory.getSkill(skillId + 1)?.let { chr.changeSkillLevel(it, 1, 1, expiration) }
            SkillFactory.getSkill(skillId + 2)?.let { chr.changeSkillLevel(it, 1, 1, expiration) }
        } else {
            SkillFactory.getSkill(skillId)?.let { chr.changeSkillLevel(it, 2, 2, chr.getSkillExpiration(skillId)) }
        }
    }
}