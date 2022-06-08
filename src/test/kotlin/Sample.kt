package samples

import tools.PasswordHash

class PasswordHashSample {
    fun sha256() {
        val hash = PasswordHash.sha256("P@ssw0rd!", "more salt?".toByteArray())
        println("Hash: $hash")
    }
}