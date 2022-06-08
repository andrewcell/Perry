package tools.data.input

import mu.KLoggable
import java.io.InputStream

/**
 * Provides an abstract wrapper to a stream of bytes.
 */
class InputStreamByteStream(val stream: InputStream) : ByteInputStream, KLoggable {
    override val logger = logger()
    override var bytesRead = 0L

    /**
     * Reads the next byte from the stream.
     *
     * @return Then next byte in the stream.
     */
    override fun readByte(): Int {
        try {
            val temp = stream.read()
            bytesRead++
            return temp
        } catch (e: Exception) {
            logger.error(e) { "Read byte error." }
        }
        return -1
    }

    override fun available(): Long {
        return try {
            stream.available().toLong()
        } catch (e: Exception) {
            logger.error(e) { "Available error." }
            0
        }
    }
}