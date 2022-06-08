package tools.settings

data class WorldConfig(
    val flag: Int = 0,
    val serverMessage: String = "",
    val eventMessage: String = "",
    val channelCount: Int = 19,
    val recommended: Boolean = false,
    val rates: WorldRates = WorldRates(1, 1, 1)
)
