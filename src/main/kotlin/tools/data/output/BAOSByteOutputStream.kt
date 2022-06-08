package tools.data.output

import java.io.ByteArrayOutputStream

/**
 * Uses a byte array to output a stream of bytes.
 *
 */
class BAOSByteOutputStream(val bas: ByteArrayOutputStream) : ByteOutputStream {
    /**
     * Writes a byte to the stream.
     *
     * @param b The byte to write to the stream.
     * @see tools.data.output.ByteOutputStream#writeByte(byte)
     */
    override fun writeByte(b: Byte) = bas.write(b.toInt())
}