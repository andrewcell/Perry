package tools.data.output

import java.awt.Point
import java.nio.charset.Charset

open class GenericLittleEndianWriter : LittleEndianWriter {
    var bos: ByteOutputStream? = null

    /**
     * Write an array of bytes to the stream.
     *
     * @param b The bytes to write.
     */
    override fun write(b: ByteArray) {
        for (x in b.indices) {
            bos?.writeByte(b[x])
        }
    }

    /**
     * Write a byte to the stream.
     *
     * @param b The byte to write.
     */
    override fun write(b: Byte) {
        bos?.writeByte(b)
    }

    /**
     * Write a byte in integer form to the stream.
     *
     * @param b The byte as an <code>Integer</code> to write.
     */
    override fun write(b: Int) {
        bos?.writeByte(b.toByte())
    }

    override fun skip(b: Int) {
        write(ByteArray(b))
    }

    /**
     * Write a short integer to the stream.
     *
     * @param s The short integer to write.
     */
    override fun writeShort(s: Int) {
        bos?.writeByte((s and 0xFF).toByte())
        bos?.writeByte((s ushr 8 and 0xFF).toByte())
    }

    /**
     * Writes an integer to the stream.
     *
     * @param i The integer to write.
     */
    override fun writeInt(i: Int) {
        bos?.writeByte((i and 0xFF).toByte())
        bos?.writeByte((i ushr 8 and 0xFF).toByte())
        bos?.writeByte((i ushr 16 and 0xFF).toByte())
        bos?.writeByte((i ushr 24 and 0xFF).toByte())
    }

    /**
     * Writes an ASCII string the stream.
     *
     * @param s The ASCII string to write.
     */
    override fun writeASCIIString(s: String) {
        write(s.toByteArray(MS949))
    }

    override fun writeASCIIString(s: String, max: Int) {
        write(s.toByteArray(MS949))
        for (i in s.toByteArray(MS949).size until max) {
            write(0)
        }
    }

    /**
     * Writes a game-convention ASCII string to the stream.
     *
     * @param s The ASCII string to use game-convention to write.
     */
    override fun writeGameASCIIString(s: String) {
        writeShort(s.toByteArray(MS949).size.toShort().toInt())
        writeASCIIString(s)
    }

    /**
     * Writes a null-terminated ASCII string to the stream.
     *
     * @param s The ASCII string to write.
     */
    override fun writeNullTerminatedASCIIString(s: String) {
        writeASCIIString(s)
        write(0)
    }

    /**
     * Write a long integer to the stream.
     * @param l The long integer to write.
     */
    override fun writeLong(l: Long) {
        bos?.writeByte((l and 0xFF).toByte())
        bos?.writeByte((l ushr 8 and 0xFF).toByte())
        bos?.writeByte((l ushr 16 and 0xFF).toByte())
        bos?.writeByte((l ushr 24 and 0xFF).toByte())
        bos?.writeByte((l ushr 32 and 0xFF).toByte())
        bos?.writeByte((l ushr 40 and 0xFF).toByte())
        bos?.writeByte((l ushr 48 and 0xFF).toByte())
        bos?.writeByte((l ushr 56 and 0xFF).toByte())
    }

    /**
     * Writes a 2D 4 byte position information
     *
     * @param s The Point position to write.
     */
    override fun writePos(s: Point) {
        writeShort(s.x)
        writeShort(s.y)
    }

    /**
     * Writes a boolean true ? 1 : 0
     *
     * @param b The boolean to write.
     */
    override fun writeBool(b: Boolean) {
        write(if (b) 1 else 0)
    }

    companion object {
        val MS949: Charset = Charset.forName("MS949")
    }
}