package tools.data.input

/**
 * Provides an abstract interface to a stream of bytes. This stream can be seeked.
 */
interface SeekableInputStreamByteStream : ByteInputStream {
    /**
     * Seeks the stream by the specified offset.
     *
     * @param offset
     *            Number of bytes to seek.
     */
    fun seek(offset: Long)

    /**
     * Current position of the stream.
     */
    val position: Int
}