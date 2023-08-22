package tools.settings

import kotlinx.serialization.Serializable

@Serializable
data class LoggingOption(
    val directory: String = "./logs",
    val loggingLevel: String = "info",
    val maxDaysToKeep: Int = 30
)
