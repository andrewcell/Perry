package tools.data.output

import mu.KLogging
import net.SendPacketOpcode
import tools.HexTool
import tools.ServerJSON
import java.io.ByteArrayOutputStream

/**
 * Writes a client-packet little-endian stream of bytes.
 */
class PacketLittleEndianWriter(val size: Int = 32) : GenericLittleEndianWriter() {
    val baos = ByteArrayOutputStream(size)

    init {
        bos = BAOSByteOutputStream(baos)
    }

    fun opcode(sendPacketOpcode: SendPacketOpcode) = byte(sendPacketOpcode.value)

    fun getPacket(): ByteArray {
        if (ServerJSON.settings.printSendPacket) {
            logger.trace {
                """
                    Sending:
                    ${HexTool.toString(baos.toByteArray())}
                    ${HexTool.toStringFromASCII(baos.toByteArray())}
                    """.trimIndent()
            }
        }
        return baos.toByteArray()
    }

    companion object : KLogging()
}