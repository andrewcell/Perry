package server.life

import provider.Data
import provider.DataProvider
import provider.DataProviderFactory
import provider.DataTool
import provider.wz.DataType
import server.maps.AbstractLoadedLife
import tools.ServerJSON.settings
import java.io.File
import java.util.*
import kotlin.math.roundToInt

class LifeFactory {
    companion object {
        val data = DataProviderFactory.getDataProvider(File("${settings.wzPath}/Mob.wz"))
        private val stringDataWz: DataProvider = DataProviderFactory.getDataProvider(File("${settings.wzPath}/String.wz"))
        private val mobStringData: Data? = stringDataWz.getData("Mob.img")
        private val npcStringData: Data? = stringDataWz.getData("Npc.img")
        val monsterStats = mutableMapOf<Int, MonsterStats>()

        fun getLife(id: Int?, type: String?): AbstractLoadedLife? {
            return when (type) {
                "n" -> getNpc(id ?: 0)
                "m" -> getMonster(id ?: 0)
                else -> null
            }
        }

        fun getMonster(mid: Int): Monster? {
            var stats = monsterStats[mid]
            if (stats == null) {
                val monsterData = data.getData("$mid.img".padStart(11, '0')) ?: return null
                val monsterInfoData = monsterData.getChildByPath("info")
                stats = MonsterStats(
                    DataTool.getIntConvert("maxHP", monsterInfoData),
                    DataTool.getIntConvert("maxMP", monsterInfoData, 0),
                    DataTool.getIntConvert("exp", monsterInfoData, 0),
                    DataTool.getIntConvert("level", monsterInfoData),
                    DataTool.getIntConvert("removeAfter", monsterInfoData, 0),
                    DataTool.getIntConvert("boss", monsterInfoData, 0) > 0,
                    DataTool.getIntConvert("explosiveReward", monsterInfoData, 0) > 0,
                    DataTool.getIntConvert("publicReward", monsterInfoData, 0) > 0,
                    DataTool.getIntConvert("undead", monsterInfoData, 0) > 0,
                    DataTool.getString("$mid/name", mobStringData, "MISSINGNO"),
                    DataTool.getIntConvert("buff", monsterInfoData, -1),
                    DataTool.getIntConvert("getCP", monsterInfoData, 0),
                    DataTool.getIntConvert("removeOnMiss", monsterInfoData, 0) > 0,
                    DataTool.getIntConvert("dropItemPeriod", monsterInfoData, 0) * 10000,
                    DataTool.getIntConvert("hpTagColor", monsterInfoData, 0),
                    DataTool.getIntConvert("hpTagBgcolor", monsterInfoData, 0)
                )
                var special = monsterInfoData?.getChildByPath("coolDamage")
                if (special != null) {
                    val coolDmg = DataTool.getIntConvert("coolDamage", monsterInfoData)
                    val coolProb = DataTool.getIntConvert("coolDamageProb", monsterInfoData, 0)
                    stats.cool = Pair(coolDmg, coolProb)
                }
                special = monsterInfoData?.getChildByPath("loseItem")
                if (special != null) {
                    for (liData in special.children) {
                        stats.addLoseItem(
                            LoseItem(
                                DataTool.getInt(liData.getChildByPath("id")),
                                DataTool.getInt(liData.getChildByPath("prop")).toByte(),
                                DataTool.getInt(liData.getChildByPath("x")).toByte()
                            )
                        )
                    }
                }
                special = monsterInfoData?.getChildByPath("selfDestruction")
                if (special != null) {
                    stats.selfDestruction = SelfDestruction(
                        DataTool.getInt(special.getChildByPath("action")).toByte(),
                        DataTool.getIntConvert("removeAfter", special, -1),
                        DataTool.getIntConvert("hp", special, -1)
                    )
                }
                val firstAttackData = monsterInfoData?.getChildByPath("firstAttack")
                var firstAttack = 0
                if (firstAttackData != null) {
                    firstAttack = if (firstAttackData.type == DataType.FLOAT) {
                        DataTool.getFloat(firstAttackData).roundToInt()
                    } else {
                        DataTool.getInt(firstAttackData)
                    }
                }
                stats.firstAttack = firstAttack > 0
                monsterData.forEach {
                    if (it.name != "info") {
                        var delay = 0
                        it.children.forEach { pic ->
                            delay += DataTool.getIntConvert("delay", pic, 0)
                        }
                        stats.setAnimationTime(it.name, delay)
                    }
                }
                val reviveInfo = monsterInfoData?.getChildByPath("revive")
                if (reviveInfo != null) {
                    val revives: MutableList<Int> = LinkedList()
                    for (data_ in reviveInfo) {
                        revives.add(DataTool.getInt(data_))
                    }
                    stats.revives = revives
                }
                decodeElementalString(stats, DataTool.getString("elemAttr", monsterInfoData, ""))
                val monsterSkillData = monsterInfoData?.getChildByPath("skill")
                if (monsterSkillData != null) {
                    var i = 0
                    val skills: MutableList<Pair<Int, Int>> = ArrayList()
                    while (monsterSkillData.getChildByPath(i.toString()) != null) {
                        skills.add(
                            Pair(
                                Integer.valueOf(DataTool.getInt("$i/skill", monsterSkillData, 0)), Integer.valueOf(
                                    DataTool.getInt(
                                        "$i/level", monsterSkillData, 0
                                    )
                                )
                            )
                        )
                        i++
                    }
                    stats.skills = skills
                }
                val banishData = monsterInfoData?.getChildByPath("ban")
                if (banishData != null) {
                    stats.banishInfo = BanishInfo(
                        DataTool.getString("banMsg", banishData, ""),
                        DataTool.getInt("banMap/0/field", banishData, -1),
                        DataTool.getString("banMap/0/portal", banishData, "sp")
                    )
                }
                monsterStats[mid] = stats
            }
            return Monster(mid, stats)
        }

        fun getNpc(nid: Int) = Npc(nid, NpcStats(DataTool.getString("$nid/name", npcStringData, "MISSINGNO")))

        private fun decodeElementalString(stats: MonsterStats, elemAttr: String) {
            var i = 0
            while (i < elemAttr.length) {
                ElementalEffectiveness.valueOf(elemAttr.elementAt(i + 1).digitToInt())
                    ?.let { stats.setEffectiveness(Element.getFromChar(elemAttr.elementAt(i)), it) }
                i += 2
            }

        }

        data class BanishInfo(val message: String, val map: Int, val portal: String)

        data class LoseItem(val id: Int, val chance: Byte, val x: Byte)

        data class SelfDestruction(val action: Byte, val removeAfter: Int, val hp: Int)
    }
}