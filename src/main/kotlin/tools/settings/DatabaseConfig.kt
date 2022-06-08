package tools.settings

data class DatabaseConfig(
    val type: String,
    val host: String,
    val username: String,
    val password: String,
    val database: String, // file path for sqlite
    val port: Int = 3306,
    val createTableAtStart: Boolean = false
)