package tools.data.input

import mu.KLoggable
import java.io.IOException
import java.io.RandomAccessFile

/**
 * A specialized byte stream implementation that provides random access capabilities
 * using an underlying `RandomAccessFile`. This class enables reading bytes, seeking
 * to arbitrary positions, and querying the current position within the stream.
 *
 * @constructor Initializes the byte stream with the given `RandomAccessFile`.
 * @param raf The `RandomAccessFile` instance serving as the data source for the stream.
 */
class RandomAccessByteStream(val raf: RandomAccessFile) : SeekableInputStreamByteStream, KLoggable {
    /**
     * Logger instance for this class, used for logging debug, informational, warning, and error messages.
     *
     * This property is an implementation of KLoggable's logger delegation, providing a convenient way
     * to access a logger specific to the class. It can be used to record runtime events, errors, and other
     * diagnostic information for debugging and operational monitoring.
     */
    override val logger = logger()

    /**
     * Tracks the total number of bytes read from the stream.
     *
     * This variable is incremented each time a byte is successfully read from the
     * associated data source. It provides a cumulative count of the bytes consumed,
     * which can be used for logging, debugging, or monitoring the progress of data reading.
     *
     * The value is stored as a `Long` to support large streams and ensure compatibility
     * with a wide range of data sizes.
     */
    override var bytesRead = 0L

    /**
     * Reads the next byte from the underlying random access file and updates the number of bytes read.
     *
     * This method retrieves a single byte of data as an integer between 0 and 255. If the end of the file
     * is reached or an error occurs during reading, it returns -1.
     *
     * @return The next byte as an integer, or -1 if the end of the file is reached or an error occurs.
     */
    override fun readByte(): Int {
        try {
            val temp = raf.read()
            if (temp == -1) return -1
            bytesRead++
            return temp
        } catch (e: IOException) {
            logger.error(e) { "Read Byte error." }
        }
        return -1
    }

    /**
     * Moves the read/write pointer of the stream to the specified offset.
     *
     * This method sets the stream's position to the given byte offset, allowing subsequent
     * read or write operations to proceed from that position.
     *
     * @param offset The byte offset to which the stream pointer should be moved. Must be a non-negative value.
     */
    override fun seek(offset: Long) {
        raf.seek(offset)
    }

    /**
     * Represents the current position of the read/write pointer in the underlying random access file.
     *
     * The position indicates the offset (in bytes) from the beginning of the file where the next
     * read or write operation will occur. The value is retrieved from the file pointer of the
     * associated `RandomAccessFile` object and converted to an integer.
     *
     * This property is useful for tracking or modifying the location at which data is being read
     * or written in the stream.
     */
    override val position: Int
        get() = raf.filePointer.toInt()

    /**
     * Calculates the number of remaining bytes available for reading in the current stream.
     *
     * This method determines the remaining bytes by subtracting the current file pointer
     * from the total length of the underlying random access file (`RandomAccessFile`).
     * If an error occurs during the calculation, such as an I/O exception, the method logs the error
     * and returns 0.
     *
     * @return The number of remaining bytes available for reading as a `Long`, or 0 if an error occurs.
     */
    override fun available(): Long {
        return try {
            raf.length() - raf.filePointer
        } catch (e: Exception) {
            logger.error(e) { "Available error." }
            0
        }
    }
}