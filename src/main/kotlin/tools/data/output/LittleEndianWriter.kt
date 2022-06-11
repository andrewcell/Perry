package tools.data.output

import java.awt.Point

interface LittleEndianWriter {
    /**
     * Write an array of bytes to the sequence.
     *
     * @param b The bytes to write.
     */
    fun byte(b: ByteArray)

    /**
     * Write a byte to the sequence.
     *
     * @param b The byte to write.
     */
    fun byte(b: Byte)

    /**
     * Write a byte in integer form to the sequence.
     *
     * @param b The byte as an `Integer` to write.
     */
    fun byte(b: Int)

    fun skip(b: Int)

    /**
     * Writes an integer to the sequence.
     *
     * @param i The integer to write.
     */
    fun int(i: Int)

    /**
     * Write a short integer to the sequence.
     *
     * @param s The short integer to write.
     */
    fun short(s: Int)

    /**
     * Write a long integer to the sequence.
     *
     * @param l The long integer to write.
     */
    fun long(l: Long)

    /**
     * Writes an ASCII string the sequence.
     *
     * @param s The ASCII string to write.
     */
    fun ASCIIString(s: String)

    fun ASCIIString(s: String, max: Int)

    /**
     * Writes a null-terminated ASCII string to the sequence.
     *
     * @param s The ASCII string to write.
     */
    fun nullTerminatedASCIIString(s: String)

    /**
     * Writes a game-convention ASCII string to the sequence.
     *
     * @param s The ASCII string to use game-convention to write.
     */
    fun gameASCIIString(s: String)

    /**
     * Writes a 2D 4 byte position information
     *
     * @param s The Point position to write.
     */
    fun pos(s: Point)

    /**
     * Writes a boolean true ? 1 : 0
     *
     * @param b The boolean to write.
     */
    fun bool(b: Boolean)
}