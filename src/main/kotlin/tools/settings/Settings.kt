package tools.settings

import kotlinx.serialization.Serializable

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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Settings

        return events.contentEquals(other.events)
    }

    override fun hashCode(): Int {
        return events.contentHashCode()
    }
}
