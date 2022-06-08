package server.life

import provider.DataProviderFactory
import provider.DataTool
import java.io.File
import tools.ServerJSON.settings

object MobAttackInfoFactory {
    private val mobAttacks = mutableMapOf<String, MobAttackInfo>()
    private val dataSource = DataProviderFactory.getDataProvider(File("${settings.wzPath}/Mob.wz"))

    fun getMobAttackInfo(mob: Monster, attack: Int): MobAttackInfo {
        var ret = mobAttacks["${mob.id}$attack"]
        if (ret != null) return ret
        synchronized(mobAttacks) {
            mobAttacks["${mob.id}$attack"]
            if (ret == null) {
                var mobData = dataSource.getData("${mob.id}.img".padStart(11, '0'))
                if (mobData != null) {
                    mobData.getChildByPath("info")
                    val linkedMob = DataTool.getString("link", mobData, "")
                    if (linkedMob != "") {
                        mobData = dataSource.getData("$linkedMob.img".padStart(11, '0'))
                        val attackData = mobData?.getChildByPath("attack${attack + 1}/info")
                        if (attackData != null) {
                            val deadlyAttack = attackData.getChildByPath("deadlyAttack")
                            val mpBurn = DataTool.getInt("mpBurn", attackData, 0)
                            val disease = DataTool.getInt("disease", attackData, 0)
                            val level = DataTool.getInt("level", attackData, 0)
                            val mpCon = DataTool.getInt("conMP", attackData, 0)
                            ret = MobAttackInfo(mob.id, attack, deadlyAttack != null, mpBurn, disease, level, mpCon)
                        }
                    }
                }
                mobAttacks["${mob.id}$attack"] = ret!!
            }
            return ret!!
        }
    }

}