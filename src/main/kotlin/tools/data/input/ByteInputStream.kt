package tools.data.input

/**
 * Represents a generic input stream for reading byte data. This interface provides methods
 * for reading individual bytes and querying the number of available bytes and bytes that
 * have already been read.
 */
interface ByteInputStream {
    /**
     * Reads the next byte from the data source and advances the read position.
     *
     * This method retrieves a single byte of data as an integer between 0 and 255.
     * It also updates the internal counters tracking the number of bytes read.
     *
     * @return The next byte from the data source as an integer, or -1 if the end of the stream has been reached or an error occurs.
     */
    fun readByte(): Int
    /**
     * Retrieves the number of bytes that can be read from the stream without blocking.
     *
     * This method calculates the remaining bytes available for reading based on the current
     * state of the underlying byte storage or stream implementation.
     *
     * @return The number of bytes available for reading as a `Long`.
     */
    fun available(): Long
    /**
     * Tracks the total number of bytes that have been read from the stream.
     *
     * This property is updated with every read operation to reflect the cumulative
     * count of bytes processed by the stream. It is useful for monitoring the progress
     * of data consumption, debugging, or logging purposes.
     *
     * The value is represented as a `Long` to accommodate large data streams.
     */
    val bytesRead: Long
}