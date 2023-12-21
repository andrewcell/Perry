package tools.settings

import kotlinx.serialization.Serializable

/**
 * HashType is a data class that represents the type of hash used for password encryption.
 * It is annotated with @Serializable, which makes it compatible with Kotlin's serialization framework.
 * This class is typically used in the context of user authentication, where passwords need to be securely stored.
 *
 * @property hashType The type of the hash used for password encryption. Default is "pbkdf2". Other options include "sha-512" and "sha-1".
 * @property shaType The type of SHA (Secure Hash Algorithm) used. Default is "sha512".
 * @property iterationCount The number of iterations for the hash function. Default is -1, which indicates that the default number of iterations for the specified hash type should be used.
 * @property keyLength The length of the key for the hash function. Default is -1, which indicates that the default key length for the specified hash type should be used.
 */
@Serializable
data class HashType(
    val hashType: String = "pbkdf2", // sha-512, sha-1, pbkdf2
    // Above is only for PBKDF2
    val shaType: String = "sha512",
    val iterationCount: Int = -1,
    val keyLength: Int = -1
)