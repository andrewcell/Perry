package client.status

import client.Skill
import kotlinx.coroutines.Job
import server.life.MobSkill

class MonsterStatusEffect(val stati: MutableMap<MonsterStatus, Int>, val skill: Skill?, val mobSkill: MobSkill?, val monsterSkill: Boolean) {
    var cancelTask: Job? = null
    var damageSchedule: Job? = null

    fun setValue(status: MonsterStatus, newVal: Int) = stati.put(status, newVal)

    fun cancelTask() {
        cancelTask?.cancel()
        cancelTask = null
    }

    fun cancelDamageSchedule() = damageSchedule?.cancel()

    fun removeActiveStatus(stat: MonsterStatus) {
        stati.remove(stat)
    }
}