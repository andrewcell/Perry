package tools

import mu.KLoggable
import mu.KLogger
import provider.DataProvider
import provider.DataProviderFactory
import provider.DataTool
import server.life.LifeFactory
import java.io.File

object StringXmlParser : KLoggable {
    override val logger = logger()

    val itemData = mutableMapOf<Int, String>()
    val mapData = addMapEntry()
    val mobData = LifeFactory.getStringData(1)
    val npcData = LifeFactory.getStringData(2)
    val skillData = mutableMapOf<Int, String>()

    fun addItemEntry(loaded: List<Pair<Int, String>>) {
        loaded.forEach { pair ->
            itemData[pair.first] = pair.second
        }
    }

    fun addSkillEntry(id: Int, name: String) {
        skillData[id] = name
    }

    private fun addMapEntry(): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        val stringWz = DataProviderFactory.getDataProvider(File("${ServerJSON.settings.wzPath}/String.wz")).getData("Map.img")
        stringWz?.children?.forEach { data ->
            data.children.forEach {
                val mapId = it.name.toIntOrNull()
                if (mapId != null) {
                    val streetName = DataTool.getString("streetName", it)
                    val mapName = DataTool.getString("mapName", it)
                    map[mapId] = "$streetName - $mapName"

                }
            }
        //DataTool.getString(wz.name"")
        }
        return map
    }

    fun test() {
        logger.debug { "Testing String data holder - Map(100000000) - Stored Name: ${mapData[100000000]}" }
    }
}