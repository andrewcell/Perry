package tools.settings

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val type: String,
    val host: String,
    val username: String,
    val password: String,
    val database: String, // file path for sqlite
    val port: Int = 3306,
    val createTableAtStart: Boolean = false
)