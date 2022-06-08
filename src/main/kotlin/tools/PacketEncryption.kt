package tools

interface PacketEncryption {
    fun checkPacket(header: Int): Boolean
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
    fun getPacketHeader(length: Int): ByteArray
}