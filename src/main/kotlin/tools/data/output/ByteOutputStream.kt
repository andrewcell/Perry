package tools.data.output

/**
 * Provides an interface to an output stream of bytes.
 *
*/
interface ByteOutputStream {
    /**
     * Writes a byte to the stream.
     *
     * @param b The byte to write.
     */
    fun writeByte(b: Byte)
}