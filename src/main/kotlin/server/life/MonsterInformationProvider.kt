package server.life

import kotlinx.serialization.json.Json
import mu.KLoggable
import tools.ResourceFile
import tools.settings.DropData
import tools.settings.DropDataGlobal

object MonsterInformationProvider : KLoggable {
    override val logger = logger()
    private val drops: Map<Int, List<MonsterDropEntry>>
    val globalDrops = mutableListOf<MonsterGlobalDropEntry>()

    init {
        retrieveGlobal()
        val dropData = ResourceFile.load("DropData.json")?.let { Json.decodeFromString<Array<DropData>>(it) } ?: emptyArray()
        val drops = mutableMapOf<Int, List<MonsterDropEntry>>()
        dropData.forEach {
            val list = drops[it.dropperId] ?: emptyList()
            drops[it.dropperId] = list + listOf(MonsterDropEntry(it.itemId, it.chance, it.minimumQuantity, it.maximumQuantity, it.questId.toShort()))
            logger.trace { "Drop data loaded. DropperId: ${it.dropperId}, ItemId: ${it.itemId}" }
        }
        this.drops = drops
    }

    private fun retrieveGlobal() {
        try {
            val globalDropData = ResourceFile.load("DropDataGlobal.json")?.let { Json.decodeFromString<Array<DropDataGlobal>>(it) } ?: emptyArray()
            globalDropData.forEach {
                globalDrops.add(
                    MonsterGlobalDropEntry(it.itemId, it.chance, it.continent,
                        it.dropType.toByte(), it.minimumQuantity, it.maximumQuantity,
                        it.questId.toShort()
                    )
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to retrieve global drop data from resource file." }
        }
    }

    fun retrieveDrop(monsterId: Int): List<MonsterDropEntry> = drops[monsterId] ?: emptyList()

    fun clearDrops() {
        //drops.clear()
        globalDrops.clear()
        retrieveGlobal()
    }
}