package tools.settings

import kotlinx.serialization.Serializable

/**
 * DatabaseConfig is a data class that holds the configuration details for a database connection.
 * It is annotated with @Serializable, which makes it compatible with Kotlin's serialization framework.
 * This class is typically used in the context of establishing a connection to a database.
 *
 * @property type The type of the database (e.g., MySQL, SQLite).
 * @property host The host address of the database.
 * @property username The username for the database connection.
 * @property password The password for the database connection.
 * @property database The name of the database or the file path for SQLite.
 * @property port The port number for the database connection. Default is 3306.
 * @property createTableAtStart A flag indicating whether to create a table at the start of the application. Default is false.
 */
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