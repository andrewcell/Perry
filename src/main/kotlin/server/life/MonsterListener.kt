package server.life

import client.Character

interface MonsterListener {
    fun monsterKilled(monster: Monster, highestDamageChar: Character?)
}