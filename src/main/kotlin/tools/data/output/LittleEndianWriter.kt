package tools.data.output

import java.awt.Point

interface LittleEndianWriter {
    /**
     * Write an array of bytes to the sequence.
     *
     * @param b The bytes to write.
     */
    fun write(b: ByteArray)

    /**
     * Write a byte to the sequence.
     *
     * @param b The byte to write.
     */
    fun write(b: Byte)

    /**
     * Write a byte in integer form to the sequence.
     *
     * @param b The byte as an `Integer` to write.
     */
    fun write(b: Int)

    fun skip(b: Int)

    /**
     * Writes an integer to the sequence.
     *
     * @param i The integer to write.
     */
    fun writeInt(i: Int)

    /**
     * Write a short integer to the sequence.
     *
     * @param s The short integer to write.
     */
    fun writeShort(s: Int)

    /**
     * Write a long integer to the sequence.
     *
     * @param l The long integer to write.
     */
    fun writeLong(l: Long)

    /**
     * Writes an ASCII string the sequence.
     *
     * @param s The ASCII string to write.
     */
    fun writeASCIIString(s: String)

    fun writeASCIIString(s: String, max: Int)

    /**
     * Writes a null-terminated ASCII string to the sequence.
     *
     * @param s The ASCII string to write.
     */
    fun writeNullTerminatedASCIIString(s: String)

    /**
     * Writes a game-convention ASCII string to the sequence.
     *
     * @param s The ASCII string to use game-convention to write.
     */
    fun writeGameASCIIString(s: String)

    /**
     * Writes a 2D 4 byte position information
     *
     * @param s The Point position to write.
     */
    fun writePos(s: Point)

    /**
     * Writes a boolean true ? 1 : 0
     *
     * @param b The boolean to write.
     */
    fun writeBool(b: Boolean)
}