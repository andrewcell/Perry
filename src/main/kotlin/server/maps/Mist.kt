package server.maps

import client.Character
import client.Client
import client.SkillFactory
import server.StatEffect
import server.life.MobSkill
import server.life.Monster
import tools.packet.GameplayPacket
import java.awt.Point
import java.awt.Rectangle

class Mist(val mistPosition: Rectangle) : AbstractMapObject() {
    var mob: Monster? = null
    var skill: MobSkill? = null
    var owner: Character? = null
    var source: StatEffect? = null
    var isMobMist = true
    var isPoisonMist = true
    var skillDelay = 0

    constructor(mistPosition: Rectangle, mob: Monster, skill: MobSkill) : this(mistPosition) {
        this.mob = mob
        this.skill = skill
    }

    constructor(mistPosition: Rectangle, owner: Character, source: StatEffect) : this(mistPosition) {
        this.owner = owner
        this.source = source
        skillDelay = 8
        isMobMist = false
        when (source.sourceId) {
            4221006 -> isPoisonMist = false // Smoke Screen
            2111003, 12111005, 14111006 -> isPoisonMist = true // FP mist, Flame Gear, Poison bomb
        }
    }

    fun getSourceSkill() = source?.sourceId?.let { SkillFactory.getSkill(it) }

    fun makeDestroyData(): ByteArray = GameplayPacket.removeMist(objectId)

    fun makeSpawnData(): ByteArray {
        val skillA = SkillFactory.getSkill(source?.sourceId) ?: return ByteArray(0)
        return if (owner != null) {
            GameplayPacket.spawnMist(objectId, owner!!.id, getSourceSkill()!!.id,
                owner!!.getSkillLevel(skillA).toInt(), this)
        } else {
            GameplayPacket.spawnMist(objectId, mob!!.id, skill!!.skillId, skill!!.skillLevel, this)
        }
    }

    fun makeFakeSpawnData(level: Int): ByteArray {
        return owner?.let { GameplayPacket.spawnMist(objectId, it.id, getSourceSkill()!!.id, level, this) } ?:
            GameplayPacket.spawnMist(objectId, mob!!.id, skill!!.skillId, skill!!.skillLevel, this)
    }

    override val objectType = MapObjectType.MIST

    override var position: Point
        get() = mistPosition.location
        set(value) = throw UnsupportedOperationException()

    override fun sendSpawnData(client: Client) {
        client.announce(makeSpawnData())
    }

    override fun sendDestroyData(client: Client) {
        client.announce(makeDestroyData())
    }

    fun makeChanceResult() = source?.makeChanceResult() ?: false
}