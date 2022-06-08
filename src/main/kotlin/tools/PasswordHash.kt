package tools

import mu.KLogging
import org.bouncycastle.crypto.digests.SHA3Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.util.encoders.Hex
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Generate hash of string value.
 *
 * Most of the digest are allowed.
 *
 * @author A. S. Choe
 * @since 1.0.0
 */
class PasswordHash {
    companion object : KLogging() {
        /**
         * Generate hash using algorithm defined in settings file
         *
         * @param password String to hash
         * @param salt Generated salt in byte array
         * @return Generated hash in string. If failed, return empty string.
         */
        fun generate(password: String, salt: ByteArray): String {
            val hashSettings = ServerJSON.settings.hashType
            return if (hashSettings.hashType == "pbkdf2") {
                pbkdf2(password, salt, "PBKDF2WithHmac${hashSettings.shaType}", hashSettings.iterationCount, hashSettings.keyLength)
            } else {
                hashWithDigest(hashSettings.hashType, password, salt)
            }
        }

        /**
         * Generate hash using SHA-512 with salt
         *
         * @param input String to hash.
         * @param salt Salt in Byte array
         * @return Generated Hash in string
         */
        fun sha512(input: String, salt: ByteArray) = hashWithDigest("SHA-512", input, salt)

        /**
         * Generate hash using SHA-256 with salt
         *
         * @sample samples.PasswordHashSample.sha256
         * @param input String to hash.
         * @param salt Salt in Byte array
         * @return Generated Hash in string
         */
        fun sha256(input: String, salt: ByteArray) = hashWithDigest("SHA-256", input, salt)

        /**
         * Generate hash using SHA-1 with salt
         *
         * @param input String to hash.
         * @param salt Salt in Byte array
         * @return Generated Hash in string
         */
        fun sha1(input: String, salt: ByteArray) = hashWithDigest("SHA-1", input, salt)

        /**
         * Generate hash of string using provided digest.
         * If algorithm is not available, return empty string.
         *
         * @param algo Type of Hash Algorithm (ex. SHA-512, SHA-256)
         * @param raw String to hash
         * @param salt Salt in Byte array
         * @return Generated hash in String (If failed, empty)
         */
        private fun hashWithDigest(algo: String, raw: String, salt: ByteArray): String {
            try {
                // Convert byte array into sig num representation
                val bytes = MessageDigest
                    .getInstance(algo)
                    .digest((raw + salt).toByteArray())
                return Hex.toHexString(bytes)
            } catch (e: NoSuchAlgorithmException) {
                logger.error(e) { "Cannot not found specific algorithm: $algo." }
            }
            return ""
        }

        /**
         * Generate hash using PBKDF2 and desired Hmac type.
         * IF failed, return empty string.
         *
         * @param password String to hash
         * @param salt Generated salt in byte array
         * @param algo Algorithm type of PBKDF2. Default value is PBKDF2WithHmacSHA512
         * @param iterationCount Iteration count in Int. Default value is 65535
         * @param keyLength Key length. Default value is 256
         * @return Generated hash in String

         */
        private fun pbkdf2(password: String, salt: ByteArray, algo: String = "PBKDF2WithHmacSHA512", iterationCount: Int = 65535, keyLength: Int = 256): String {
            try {
                val factory = SecretKeyFactory.getInstance(algo)
                val hash = factory.generateSecret(PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength))
                return Hex.toHexString(hash.encoded)
            } catch (e: Exception) {
                logger.error(e) { "Failed to generate PBKDF2." }
            }
            return ""
        }

        /**
         * Generate hash using PBKDF2 with SHA3.
         * If failed, return empty string.
         *
         * @param password String to hash
         * @param salt Generated salt in byte array
         * @param iteration Iteration count in int
         * @return Generated hash in String
         */
        private fun pbkdf2SHA3(password: String, salt: ByteArray, iteration: Int): String {
            try {
                val generator = PKCS5S2ParametersGenerator(SHA3Digest(256))
                generator.init(password.toByteArray(), salt, iteration)
                val derivedKey = (generator.generateDerivedParameters(32 * 8) as KeyParameter).key
                return Hex.toHexString(derivedKey)
            } catch (e: Exception) {
                logger.error(e) { "Failed to generate PBKDF2." }
            }
            return ""
        }

        /**
         * Generate salt for PBKDF2 or others
         *
         * @param seedBytes Seed bytes of salt. Default is 16. Use default value if you're not Digest Experts.
         * @return Byte array of generated salt
         */
        fun generateSalt(seedBytes: Int = 16): ByteArray {
            val random = SecureRandom()
            return random.generateSeed(seedBytes)
        }
    }
}