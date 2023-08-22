package tools.settings

import kotlinx.serialization.Serializable

@Serializable
data class ReactorDropDatabase(
    val reactorDropId: Int,
    val reactorId: Int,
    val itemId: Int,
    val change: Int,
    val questId: Int
)