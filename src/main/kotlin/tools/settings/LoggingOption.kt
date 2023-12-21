package tools.settings

import kotlinx.serialization.Serializable

/**
 * LoggingOption is a data class that holds the configuration details for logging in the application.
 * It is annotated with @Serializable, which makes it compatible with Kotlin's serialization framework.
 * This class is typically used in the context of configuring how the application logs information.
 *
 * @property directory The directory where the log files will be stored. Default is "./logs".
 * @property loggingLevel The level of logging. This determines the severity of the messages that will be logged. Default is "info".
 * @property maxDaysToKeep The maximum number of days to keep the log files. Log files older than this number of days will be deleted. Default is 30.
 */
@Serializable
data class LoggingOption(
    val directory: String = "./logs",
    val loggingLevel: String = "info",
    val maxDaysToKeep: Int = 30
)