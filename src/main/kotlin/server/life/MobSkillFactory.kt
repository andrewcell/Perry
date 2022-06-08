package server.life

import provider.DataProviderFactory
import provider.DataTool
import java.awt.Point
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import tools.ServerJSON.settings

class MobSkillFactory {
    companion object {
        fun getMobSkill(skillId: Int, level: Int): MobSkill {
            val key = "$skillId$level"
            dataLock.readLock().lock()
            try {
                val ret = mobSkills[key]
                if (ret != null) return ret
            } finally {
                dataLock.readLock().unlock()
            }
            dataLock.writeLock().lock()
            try {
                var ret = mobSkills[key]
                if (ret == null) {
                    val skillData = skillRoot?.getChildByPath("$skillId/level/$level")
                    if (skillData != null) {
                        val mpCon = DataTool.getInt(skillData.getChildByPath("mpCon"), 0)
                        val toSummon = mutableListOf<Int>()
                        var i = 0
                        while (i > -1) {
                            if (skillData.getChildByPath(i.toString()) == null) break
                            toSummon.add(DataTool.getInt(skillData.getChildByPath(i.toString()), 0))
                            i++
                        }
                        val ltd = skillData.getChildByPath("lt")
                        ret = MobSkill(skillId, level, DataTool.getInt("summonEffect", skillData, 0),
                            hp = DataTool.getInt("hp", skillData, 100), mpCon = mpCon,
                            x = DataTool.getInt("x", skillData, 1), y = DataTool.getInt("y", skillData, 1),
                            duration = (DataTool.getInt("time", skillData, 0) * 1000).toLong(), coolTime = (DataTool.getInt("interval", skillData, 0) * 1000).toLong(),
                            prop = ((DataTool.getInt("prop", skillData, 100)) / 100).toFloat(), limit = DataTool.getInt("limit", skillData, 0),
                            lt = ltd?.data as Point, rb = skillData.getChildByPath("rb")?.data as Point
                        )
                        ret.addSummons(toSummon)
                    }
                    mobSkills["$skillId$level"] = ret!!
                }
                return ret
            } finally {
                dataLock.writeLock().unlock()
            }
        }
        private val mobSkills = mutableMapOf<String, MobSkill>()
        private val dataSource = DataProviderFactory.getDataProvider(File("${settings.wzPath}/Skill.wz"))
        private val skillRoot = dataSource.getData("MobSkill.img")
        private val dataLock = ReentrantReadWriteLock()
    }
}