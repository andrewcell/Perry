package tools

import mu.KLoggable
import provider.DataProviderFactory
import provider.DataTool
import server.life.LifeFactory
import webapi.controller.SearchType
import java.io.File

/**
 * To search resources in game by name or id, we need to parse String.wz and store them in some places.
 * This will be store string data, and provide search function. This can be used in GM command, or Web API.
 * Open to non-GM users are not recommended. Each search function require additional computing power from the server.
 * Please check out AdminSearch controller before use.
 *
 * @see webapi.controller.adminSearch
 * @author Seungyeon Choi
 */
object StringXmlParser : KLoggable {
    override val logger = logger()

    val itemData = mutableMapOf<Int, String>()
    val mapData = getMapEntry()
    val mobData = LifeFactory.getStringData(1)
    val npcData = LifeFactory.getStringData(2)
    val skillData = mutableMapOf<Int, String>()

    /**
     * Function to add items. When server in starting process, all items are going to load once, use this function to fill
     *
     * @param loaded List of a pair of id and name. Items are actually separated by inventory type or category.
     * This will be the one set of list by type.
     */
    fun addItemEntry(loaded: List<Pair<Int, String>>) {
        loaded.forEach { pair ->
            itemData[pair.first] = pair.second
        }
    }

    /**
     * Function to add skills. When server in starting process, all skills are going to load once, use this function to fill
     *
     * @param id Id of skill
     * @param name Name of skill
     */
    fun addSkillEntry(id: Int, name: String) {
        skillData[id] = name
    }

    /**
     * Parse String.wz\Map.img.xml and get map of code and name
     *
     * @return Map of code and name. name will be combined as "StreetName - MapName"
     */
    private fun getMapEntry(): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        val stringWz =
            DataProviderFactory.getDataProvider(File("${ServerJSON.settings.wzPath}/String.wz")).getData("Map.img")
        stringWz?.children?.forEach { data ->
            data.children.forEach {
                val mapId = it.name.toIntOrNull()
                if (mapId != null) {
                    val streetName = DataTool.getString("streetName", it)
                    val mapName = DataTool.getString("mapName", it)
                    map[mapId] = "$streetName - $mapName"

                }
            }
        }
        return map
    }

    /**
     * Find something using type, code or Id, name. Search by string contains
     *
     * @param type type of search. check SearchType
     * @see webapi.controller.SearchType
     * @param code Integer value of code
     * @param name query string of name
     * @return Map of code and name
     */
    fun find(type: SearchType, code: Int? = null, name: String? = null): Map<Int, String> {
        if (code == null && name == null) return emptyMap()
        return when (type) {
            SearchType.MAP -> mapData
            SearchType.ITEM -> itemData
            SearchType.NPC -> npcData
            SearchType.SKILL -> skillData
            SearchType.MOB -> mobData
        }.filter { (key, string) ->
            code?.let { key.toString().contains(it.toString()) } == true
                    || name?.let { string.contains(it) } == true
        }
    }

    /**
     * Test function to print loaded data to Log. For test
     */
    fun test() {
        logger.debug { "Testing String data holder - Map(100000000) - Stored Name: ${mapData[100000000]}" }
    }
}