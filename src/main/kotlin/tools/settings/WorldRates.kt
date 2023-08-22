package tools.settings

import kotlinx.serialization.Serializable

@Serializable
data class WorldRates(
    val exp: Int = 1,
    val meso: Int = 1,
    val drop: Int = 1,
    val bossDrop: Int = 1
)
