package constants

class ServerConstants {
    companion object {
        const val gameVersion: Short = 31
        const val patchVersion: Byte = 1
        const val itemId = 3993003
        val worldNames = listOf("스카니아", "베라", "브로아", "카이니", "제니스", "크로아", "아케니아", "마르디아", "플라나", "스티어스", "벨로칸", "데메토스", "옐론드", "카스티아", "엘니도", "윈디아", "쥬디스", "카디아", "갈리시아", "칼루나", "테스피아")
        const val questExpRate = 1
        const val questMesoRate = 1
        const val channelLoad = 100 // Players per channel
        const val autoSaveInterval = 1800000
        const val rankingInterval = 1800000
        const val perfectPitch = true
    }
}