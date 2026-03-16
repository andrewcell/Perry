package tools.data.input

import tools.HexTool

/**
 * A byte stream implementation that operates on a given byte array. This class provides
 * methods for reading bytes, seeking within the array, and inspecting the stream state.
 *
 * @constructor Initializes the byte stream with the given byte array.
 * @param array The byte array to serve as the source of the stream.
 */
class ByteArrayByteStream(val array: ByteArray) : SeekableInputStreamByteStream {
    /**
     * Represents the current position of the stream within the byte array.
     * This variable tracks the index of the next byte to be read. It can
     * be updated directly or via stream operations such as seeking or reading.
     */
    override var position = 0
    /**
     * Tracks the total number of bytes that have been read from the stream.
     *
     * This variable is incremented with every read operation to maintain an accurate
     * count of how many bytes have been consumed. It provides a way to monitor or audit
     * the amount of data processed, which can be useful for debugging, logging, or
     * implementing custom behaviors based on read progress.
     *
     * The value is represented as a `Long` to support large streams of data.
     */
    override var bytesRead = 0L

    /**
     * Sets the position of the stream to the specified offset.
     *
     * @param offset The byte offset to which the position should be set. The value is converted to an integer.
     */
    override fun seek(offset: Long) {
        position = offset.toInt()
    }

    /**
     * Reads the next byte from the byte array and increments the position and bytesRead counters.
     *
     * @return The next byte in the byte array as an integer value between 0 and 255.
     */
    override fun readByte(): Int {
        bytesRead++
        return array[position++].toInt() and 0xFF
    }

    /**
     * Returns a string representation of the current state of the byte stream.
     *
     * @return A string containing the hexadecimal representation of the entire byte array
     *         and the remaining byte array from the current position.
     */
    override fun toString(): String {
        var str = "END OF STRING"
        if (array.size - position > 0) {
            val now = ByteArray(array.size - position)
            System.arraycopy(array, position, now, 0, array.size - position)
            str = HexTool.toString(now)
        }
        return "ALL: ${HexTool.toString(array)}\nNow: $str"
    }

    /**
     * Calculates the number of bytes available for reading from the stream.
     *
     * This method computes the remaining bytes by subtracting the current position
     * in the byte stream from the total size of the byte array backing the stream.
     *
     * @return The number of bytes available for reading as a `Long`.
     */
    override fun available() = (array.size - position).toLong()
}