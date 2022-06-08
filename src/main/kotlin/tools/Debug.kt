package tools

import org.bouncycastle.util.encoders.Hex

fun main(args: Array<String>) {
    val salt = PasswordHash.generateSalt()
    val hex = salt

    println(Hex.toHexString(salt))
    println(Hex.decode(hex) contentEquals salt)
}

