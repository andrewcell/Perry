package tools.data.input

import mu.KLoggable
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Provides an abstract layer to a byte stream. This layer can be accessed randomly.
 */
class RandomAccessByteStream(val raf: RandomAccessFile) : SeekableInputStreamByteStream, KLoggable {
    override val logger = logger()
    override var bytesRead = 0L

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

    override fun seek(offset: Long) {
        raf.seek(offset)
    }

    override val position: Int
        get() = raf.filePointer.toInt()

    override fun available(): Long {
        return try {
            raf.length() - raf.filePointer
        } catch (e: Exception) {
            logger.error(e) { "Available error." }
            0
        }
    }
}