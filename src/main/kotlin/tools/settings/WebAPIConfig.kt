package tools.settings

import kotlinx.serialization.Serializable

@Serializable
data class WebAPIConfig(
    val enable: Boolean = false,
    val port: Int = 9090
)