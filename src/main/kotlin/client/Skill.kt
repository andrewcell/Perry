package client

import server.StatEffect
import server.life.Element

class Skill(val id: Int) {
    val effects = mutableListOf<StatEffect>()
    var element: Element? = null
    var animationTime = -1
    var action = false
    var coolTime = -1

    fun getEffect(level: Int) = effects[level - 1]

    fun getMaxLevel() = effects.size

    fun isFourthJob() = (id / 10000) % 10 == 2

    fun isBeginnerSkill() = id % 10000000 < 10000

}