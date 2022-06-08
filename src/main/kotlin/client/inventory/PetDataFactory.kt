package client.inventory

import provider.DataProviderFactory
import provider.DataTool
import java.io.File
import tools.ServerJSON.settings

class PetDataFactory {
    companion object {
        val dataRoot = DataProviderFactory.getDataProvider(File("${settings.wzPath}/Item.wz"))
        val petCommands = mutableMapOf<String, PetCommand>()
        val petHunger = mutableMapOf<Int, Int>()

        fun getPetCommand(petId: Int, skillId: Int): PetCommand {
            var et = petCommands["$petId$skillId"]
            if (et != null) return et
            var ret: PetCommand
            synchronized(petCommands) {
                val cc = petCommands["$petId$skillId"]
                if (cc == null) {
                    val skillData = dataRoot.getData("Pet/$petId.img")
                    var prob = 0
                    var inc = 0
                    if (skillData != null) {
                        prob = DataTool.getInt("interact/$skillId/prob", skillData, 0)
                        inc = DataTool.getInt("interact/$skillId/inc", skillData, 0)
                    }
                    val new = PetCommand(petId, skillId, prob, inc)
                    petCommands["$petId$skillId"] = new
                    return new
                }
                ret = cc
            }
            return ret
        }

        fun getHunger(petId: Int): Int {
            val ret = petHunger[petId]
            if (ret != null) return ret
            synchronized(petHunger) {
                val value = DataTool.getInt(dataRoot.getData("Pet/$petId.img")?.getChildByPath("info/hungry"), 1)
                petHunger[petId] = value
                return value
            }
        }
    }
}