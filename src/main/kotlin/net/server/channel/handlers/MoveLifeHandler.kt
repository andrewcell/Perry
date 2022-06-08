package net.server.channel.handlers

import client.Client
import server.life.MobSkill
import server.life.MobSkillFactory
import server.life.Monster
import server.maps.MapObjectType
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket
import java.awt.Point
import kotlin.random.Random

class MoveLifeHandler : AbstractMovementPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val objectId = slea.readInt()
        val moveId = slea.readShort()
        val mmo = c.player?.map?.mapObjects?.get(objectId)
        if (mmo == null || mmo.objectType != MapObjectType.MONSTER) return
        mmo as Monster
        val skillByte = slea.readByte() //pass
        val skill = slea.readByte() //Encode1
        val skill1: Int = slea.readByte().toInt() and 0xFF //Encode1
        val skill2 = slea.readByte() //Encode4
        val skill3 = slea.readByte()
        val skill4 = slea.readByte()
        var toUse: MobSkill? = null
        if (skillByte.toInt() == 1 && mmo.stats.skills.size > 0) {
            val random = Random.nextInt(mmo.stats.skills.size)
            val skillToUse = mmo.stats.skills[random]
            toUse = MobSkillFactory.getMobSkill(skillToUse.first, skillToUse.second)
            val perHpLeft = (mmo.hp / mmo.stats.hp) * 100
            if (toUse.hp < perHpLeft || !mmo.canUseSkill(toUse)) {
                toUse = null
            }
        }
        if ((skill1 in 100..200) && mmo.hasSkill(skill1, skill2.toInt())) {
            val skillData = MobSkillFactory.getMobSkill(skill1, skill2.toInt())
            if (mmo.canUseSkill(skillData)) {
                c.player?.let { skillData.applyEffect(it, mmo, true) }
            }
        }
        val startX = slea.readShort()
        val startY = slea.readShort()
        val startPos = Point(startX.toInt(), startY.toInt())
        val res = parseMovement(slea)
        slea.readByte()
        c.player?.let { player ->
            if (mmo.controller?.get() != player) {
                if (mmo.isAttackedBy(player)) {
                    mmo.switchController(player, true)
                } else return
            } else if (skill.toInt() == -1 && mmo.isControllerKnowsAboutAggro() && !mmo.stats.isMobile() && !mmo.stats.firstAttack) {
                mmo.controllerHasAggro = false
                mmo.controllerKnowsAboutAggro = false
            }
            val aggro = mmo.controllerHasAggro
            if (toUse != null) {
                c.announce(GameplayPacket.moveMonsterResponse(objectId, moveId, mmo.mp, aggro, toUse.skillId, toUse.skillLevel))
            } else {
                c.announce(GameplayPacket.moveMonsterResponse(objectId, moveId, mmo.mp, aggro))
            }
            if (aggro) {
                mmo.controllerKnowsAboutAggro = true
            }
            if (res != null) {
                player.map.broadcastMessage(player, GameplayPacket.moveMonster(skillByte, skill, skill1, skill2, skill3, skill4, objectId, startPos, res), mmo.position)
                updatePosition(res, mmo, -1)
                player.map.moveMonster(mmo, mmo.position)
            }
        }
    }
}