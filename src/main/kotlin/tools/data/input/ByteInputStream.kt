package tools.data.input

/**
 * Represents on abstract stream of bytes.
 */
interface ByteInputStream {
    fun readByte(): Int
    fun available(): Long
    val bytesRead: Long
}