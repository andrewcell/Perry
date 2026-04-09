package tools.data.output

import io.github.oshai.kotlinlogging.KotlinLogging
import net.SendPacketOpcode
import tools.HexTool
import tools.ServerJSON
import java.io.ByteArrayOutputStream

/**
 * Class responsible for writing packets in Little Endian format for network communication.
 * It utilizes a dynamic byte array as the underlying storage and provides functionality
 * to add opcode and retrieve the composed packet as a byte array.
 *
 * @param size The initial size of the underlying byte array buffer. Defaults to 32.
 */
class PacketLittleEndianWriter(val size: Int = 32) : GenericLittleEndianWriter() {
    private val logger = KotlinLogging.logger {  }

    /**
     * A `ByteArrayOutputStream` instance used as an underlying buffer for
     * constructing packets in little-endian format. The buffer size is initialized
     * based on the specified `size` parameter of the containing class.
     *
     * This variable is crucial for writing and accumulating byte data that will
     * later be retrieved as a byte array, allowing seamless serialization of
     * packet data. It serves as the core data container for the packet construction
     * process in the `PacketLittleEndianWriter` class.
     */
    val baos = ByteArrayOutputStream(size)

    init {
        bos = BAOSByteOutputStream(baos)
    }

    /**
     * Writes the opcode of the specified packet to the output stream.
     *
     * @param sendPacketOpcode The opcode of the packet to be written, represented by the `SendPacketOpcode` enum.
     */
    fun opcode(sendPacketOpcode: SendPacketOpcode) = byte(sendPacketOpcode.value)

    /**
     * Returns the packet as a byte array. If the corresponding debug setting is enabled,
     * logs the packet content in both hexadecimal and ASCII formats.
     *
     * @return The packet represented as a byte array.
     */
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
}