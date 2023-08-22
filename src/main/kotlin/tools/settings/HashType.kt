package tools.settings

import kotlinx.serialization.Serializable

@Serializable
data class HashType(
    val hashType: String = "pbkdf2", // sha-512, sha-1, pbkdf2
    // Above is only for PBKDF2
    val shaType: String = "sha512",
    val iterationCount: Int = -1,
    val keyLength: Int = -1
)
