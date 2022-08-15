package webapi.tools

class JWTVariables {
    companion object {
        val secret = System.getenv("jwt.secret") ?: generateRandomString()
        val issuer = System.getenv("jwt.issuer") ?: generateRandomString()
        val audience = System.getenv("jwt.audience") ?: generateRandomString()
        val myRealm = System.getenv("jwt.realm") ?: generateRandomString()

        private fun generateRandomString(): String {
            val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
            return List(16) {
                charPool.random()
            }.joinToString("")
        }
    }
}