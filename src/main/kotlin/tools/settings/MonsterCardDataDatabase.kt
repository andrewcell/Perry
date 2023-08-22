package tools.settings

import kotlinx.serialization.Serializable

@Serializable
data class MonsterCardDataDatabase(val id: Int, val cardId: Int, val mobId: Int)
