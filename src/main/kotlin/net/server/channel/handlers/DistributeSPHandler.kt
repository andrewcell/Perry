package net.server.channel.handlers

import client.CharacterStat
import client.Client
import client.SkillFactory
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class DistributeSPHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val skillId = slea.readInt()
        val player = c.player ?: return
        var remainingSp = player.remainingSp
        var isBeginnerSkill = false
        if (skillId % 10000000 in 1000..1002) {
            var total = 0
            for (i in 0..2) {
                total += player.getSkillLevel(SkillFactory.getSkill(player.getJobType() * 10000000 + 1000 + i)!!)
            }
            remainingSp = Math.min(player.level - 1, 6) - total
            isBeginnerSkill = true
        }
        val skill = SkillFactory.getSkill(skillId) ?: return
        val curLevel = player.getSkillLevel(skill)
        if (remainingSp > 0 && curLevel + 1 <= if (skill.isFourthJob()) player.getMasterLevel(skill) else skill.getMaxLevel()) {
            if (!isBeginnerSkill) player.remainingSp = player.remainingSp - 1
            player.updateSingleStat(CharacterStat.AVAILABLESP, player.remainingSp, false)
            player.changeSkillLevel(skill, (curLevel + 1).toByte(), player.getMasterLevel(skill), player.getSkillExpiration(skill))
        }
    }
}