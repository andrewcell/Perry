package server.quest

enum class QuestRequirementType(val type: Byte) {
    UNDEFINED(-1), JOB(0), ITEM(1), QUEST(2), MIN_LEVEL(3),
    MAX_LEVEL(4), END_DATE(5), MOB(6), NPC(7), FIELD_ENTER(8),
    INTERVAL(9), SCRIPT(10), PET(11), MIN_PET_TAMENESS(12), MONSTER_BOOK(13),
    NORMAL_AUTO_START(14), INFO_NUMBER(15), INFO_EX(16), COMPLETED_QUEST(17);

    companion object {
        fun getByWzName(name: String) = when (name) {
            "job" -> JOB
            "quest" -> QUEST
            "item" -> ITEM
            "lvmin" -> MIN_LEVEL
            "lvmax" -> MAX_LEVEL
            "end" -> END_DATE
            "mob" -> MOB
            "npc" -> NPC
            "fieldEnter" -> FIELD_ENTER
            "interval" -> INTERVAL
            "startscript" -> SCRIPT
            "endscript" -> SCRIPT
            "pet" -> PET
            "pettamenessmin" -> MIN_PET_TAMENESS
            "mbmin" -> MONSTER_BOOK
            "normalAutoStart" -> NORMAL_AUTO_START
            "infoNumber" -> INFO_NUMBER
            "infoex" -> INFO_EX
            "questComplete" -> COMPLETED_QUEST
            else -> UNDEFINED
        }
    }
}