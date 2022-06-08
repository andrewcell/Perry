package server.quest

enum class QuestActionType(val type: Byte) {
    UNDEFINED(-1), EXP(0), ITEM(1), NEXT_QUEST(2), MESO(3),
    QUEST(4), SKILL(5), FAME(6), BUFF(7), PETSKILL(8),
    YES(9), NO(10), NPC(11), MIN_LEVEL(12), NORMAL_AUTO_START(13), ZERO(14);

    companion object {
        fun getByWzName(name: String): QuestActionType = when (name) {
            "exp" -> EXP
            "money" -> MESO
            "item" -> ITEM
            "skill" -> SKILL
            "nextQuest" -> NEXT_QUEST
            "pop" -> FAME
            "buffItemID" -> BUFF
            "petskill" -> PETSKILL
            "no" -> NO
            "yes" -> YES
            "npc" -> NPC
            "lvmin" -> MIN_LEVEL
            "normalAutoStart" -> NORMAL_AUTO_START
            "0" -> ZERO
            else -> UNDEFINED
        }
    }
}