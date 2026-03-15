package constants

class ServerConstants {
    companion object {
        const val GAME_VERSION: Short = 31
        const val MINOR_VERSION: Byte = 1
//        const val itemId = 3993003
        val worldNames = listOf("스카니아", "베라", "브로아", "카이니", "제니스", "크로아", "아케니아", "마르디아", "플라나", "스티어스", "벨로칸", "데메토스", "옐론드", "카스티아", "엘니도", "윈디아", "쥬디스", "카디아", "갈리시아", "칼루나", "테스피아")
        const val QUEST_EXP_RATE = 1
        const val QUEST_MESO_RATE = 1
        const val MAX_PLAYERS_PER_CHANNEL = 100 // Players per channel
        const val AUTOSAVE_INTERVAL = 1800000
        const val RANKING_INTERVAL = 1800000
        const val PERFECT_PITCH = true //TODO: Find this thing what actually doing
    }
}