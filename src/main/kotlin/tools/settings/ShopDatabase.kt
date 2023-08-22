package tools.settings

import kotlinx.serialization.Serializable

@Serializable
data class ShopDatabase(
    val shopId: Int,
    val npcId: Int
)
