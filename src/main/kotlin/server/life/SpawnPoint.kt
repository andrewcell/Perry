package server.life

import client.Character
import java.awt.Point
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class SpawnPoint(monster: Monster, val pos: Point, private val imMobile: Boolean, val mobTime: Int, val mobInterval: Int, private val team: Int) {
    val fh = monster.fh
    val f = monster.f
    private val monsterId = monster.id
    var nextPossibleSpawn = System.currentTimeMillis()
    val spawnedMonsters = AtomicInteger(0)
    val position = Point(pos)

    fun shouldSpawn(): Boolean {
        return if (mobTime < 0 || ((mobTime != 0 || imMobile) && spawnedMonsters.get() > 0) || spawnedMonsters.get() > 2)
            return false
        else
            nextPossibleSpawn <= System.currentTimeMillis()
    }

    fun getMonster(): Monster? {
        val m = LifeFactory.getMonster(monsterId) ?: return null
        val mob = Monster(m)
        mob.position = Point(position)
        mob.team = team
        mob.fh = fh
        mob.f = f
        spawnedMonsters.incrementAndGet()
        mob.addListener(object : MonsterListener {
            override fun monsterKilled(monster: Monster, highestDamageChar: Character?) {
                nextPossibleSpawn = System.currentTimeMillis() + max((mobTime * 1000).coerceAtLeast(mobInterval), 5000)
                spawnedMonsters.decrementAndGet()
            }
        })
        return mob
    }
}