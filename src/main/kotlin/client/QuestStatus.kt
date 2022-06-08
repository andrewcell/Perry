package client

import server.quest.Quest
import tools.AtomicString

class QuestStatus(val quest: Quest, var status: Status, val npc: Int = -1) {
    enum class Status(val id: Int) {
        UNDEFINED(-1),
        NOT_STARTED(0),
        STARTED(1),
        COMPLETED(2);

        companion object {
            fun getById(id: Int) = values().find { it.id == id }
        }
    }
    var completionTime = System.currentTimeMillis()
    val progresses = mutableMapOf<Int, AtomicString>()
    val medalProgress = mutableListOf<Int>()
    var forfeited = 0
        set(value) {
            field = if (field >= value) field else value
        }

    init {
        if (status == Status.STARTED) registerMobs()
    }

    fun addMedalMap(mapId: Int): Boolean {
        if (medalProgress.contains(mapId)) return false
        medalProgress.add(mapId)
        return true
    }

    fun getMedalProgressSize() = medalProgress.size

    fun getProgress(id: Int) = progresses[id]?.get() ?: ""

    fun getQuestData(): String {
        return buildString {
            progresses.forEach { (_, u) ->
                append(u.get())
            }
        }
    }

    fun progress(id: Int): Boolean {
        val value = progresses[id]
        return if (value != null) {
            val current = value.get().toInt()
            val str2 = (current + 1).toString().padStart(3, '0')
            progresses[id] = AtomicString(str2)
            true
        } else false
    }

    private fun registerMobs() {
        quest.relevantMobs.forEach {
            progresses[it] = AtomicString("000")
        }
    }

    fun setProgress(id: Int, pr: String) = progresses.set(id, AtomicString(pr))
}