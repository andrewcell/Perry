package tools.settings

import kotlinx.serialization.Serializable

/**
 * Settings is a data class that holds the configuration details for the application.
 * It is annotated with @Serializable, which makes it compatible with Kotlin's serialization framework.
 * This class is typically used in the context of configuring the application's settings.
 *
 * @property host The host address of the server. Default is "127.0.0.1".
 * @property bindHost The host address that the server binds to. Default is "0.0.0.0".
 * @property bindPort The port number that the server binds to. Default is 8484.
 * @property enableGMServer A flag indicating whether the GM server is enabled. Default is false.
 * @property wzPath The path to the WZ files. Default is "wz/".
 * @property database The configuration details for the database connection.
 * @property worlds The list of world configurations.
 * @property webApi The configuration details for the web API. Default is an instance of WebAPIConfig.
 * @property printSendPacket A flag indicating whether to print sent packets. Default is false.
 * @property printReceivePacket A flag indicating whether to print received packets. Default is false.
 * @property modifiedClient A flag indicating whether the client is modified. Default is false.
 * @property autoRegister A flag indicating whether auto registration is enabled. Default is false.
 * @property events An array of events.
 * @property hashType The type of hash used for password encryption. Default is an instance of HashType.
 * @property logging The configuration details for logging. Default is an instance of LoggingOption.
 */
@Serializable
data class Settings(
    val host: String = "127.0.0.1",
    val bindHost: String = "0.0.0.0",
    val bindPort: Int = 8484,
    val enableGMServer: Boolean = false,
    val wzPath: String = "wz/",
    val database: DatabaseConfig,
    val worlds: List<WorldConfig>,
    val webApi: WebAPIConfig = WebAPIConfig(),
    val printSendPacket: Boolean = false,
    val printReceivePacket: Boolean = false,
    val modifiedClient: Boolean = false,
    val autoRegister: Boolean = false,
    val events: Array<String> = arrayOf(),
    val hashType: HashType = HashType(),
    val logging: LoggingOption = LoggingOption()
) {
    /**
     * Checks if the given object is equal to this Settings object.
     * Two Settings objects are considered equal if their events arrays are equal.
     *
     * @param other The object to compare with this Settings object.
     * @return True if the given object is equal to this Settings object, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Settings

        return events.contentEquals(other.events)
    }

    /**
     * Generates a hash code for this Settings object.
     * The hash code is based on the content of the events array.
     *
     * @return The hash code of this Settings object.
     */
    override fun hashCode(): Int {
        return events.contentHashCode()
    }
}