package tools

import net.SendPacketOpcode
import org.bouncycastle.util.encoders.Hex
import tools.data.output.PacketLittleEndianWriter

fun main(args: Array<String>) {
    val salt = PasswordHash.generateSalt()
    val hex = salt
    fun ww(w: PacketLittleEndianWriter.() -> Unit): ByteArray {
        return PacketLittleEndianWriter().apply(w).getPacket()
    }


    val s = ww {
        byte(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.value)
    }
    println(s.toString())
    println(Hex.toHexString(salt))
    println(Hex.decode(hex) contentEquals salt)
}

