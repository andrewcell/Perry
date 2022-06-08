package server.maps

import client.Character
import client.Client
import client.SkillFactory
import tools.packet.GameplayPacket
import java.awt.Point

class Summon(val owner: Character, val skill: Int, pos: Point, val movementType: SummonMovementType) : AbstractAnimatedMapObject() {
    val skillLevel = SkillFactory.getSkill(skill)?.let { owner.getSkillLevel(it) } ?: 0
    var hp = 0

    init {
        position = pos
    }

    fun isStationary() = when (skill) {
        311002, 3211002, 5211001, 13111004 -> true
        else -> false
    }

    fun isPuppet() = when (skill) {
        3111002, 3211002, 13111004 -> true
        else -> false
    }

    fun addHp(value: Int) {
        hp += value
    }

    override val objectType = MapObjectType.SUMMON

    override fun sendDestroyData(client: Client) {
        client.announce(GameplayPacket.removeSummon(this, true))
    }

    override fun sendSpawnData(client: Client) {
        client.announce(GameplayPacket.spawnSummon(this, false))
    }
}