package tools.data.input

import java.awt.Point
import java.io.ByteArrayOutputStream
import java.lang.Double.longBitsToDouble
import java.lang.Float.intBitsToFloat
import java.nio.charset.Charset

/**
 * Provides a generic interface to a Little Endian stream of bytes.
 *
*/
open class GenericLittleEndianAccessor(open val bs: ByteInputStream) : LittleEndianAccessor {
    /**
     * Read a single byte from the stream.
     *
     * @return The byte read.
     */
    override fun readByte() = bs.readByte().toByte()

    /**
     * Reads an integer from the stream.
     *
     * @return The integer read.
     */
    override fun readInt(): Int {
        val a = bs.readByte()
        val b = bs.readByte()
        val c = bs.readByte()
        val d = bs.readByte()
        return a + (b shl 8) + (c shl 16) + (d shl 24)
    }


    /**
     * Reads a short integer from the stream.
     *
     * @return The short read.
     */
    override fun readShort(): Short =
        (bs.readByte() + (bs.readByte() shl 8)).toShort()

    /**
     * Reads a single character from the stream.
     *
     * @return The character read.
     */
    override fun readChar(): Char = readShort().toInt().toChar()

    /**
     * Reads a long integer from the stream.
     *
     * @return The long integer read.
     */
    override fun readLong(): Long {
        val byte1 = bs.readByte().toLong()
        val byte2 = bs.readByte().toLong()
        val byte3 = bs.readByte().toLong()
        val byte4 = bs.readByte().toLong()
        val byte5 = bs.readByte().toLong()
        val byte6 = bs.readByte().toLong()
        val byte7 = bs.readByte().toLong()
        val byte8 = bs.readByte().toLong()
        return (byte8 shl 56) + (byte7 shl 48) + (byte6 shl 40) + (byte5 shl 32) + (byte4 shl 24) + (byte3 shl 16) + (byte2 shl 8) + byte1
    }

    /**
     * Reads a floating point integer from the stream.
     *
     * @return The float-type integer read.
     */
    override fun readFloat(): Float = intBitsToFloat(readInt())

    /**
     * Reads a double-precision integer from the stream.
     *
     * @return The double-type integer read.
     */
    override fun readDouble(): Double = longBitsToDouble(readLong())

    /**
     * Reads an ASCII string from the stream with length <code>n</code>.
     *
     * @param n Number of characters to read.
     * @return The string read.
     */
    override fun readASCIIString(n: Int): String {
        val ret = ByteArray(n)
        for (x in 0 until n) {
            ret[x] = readByte()
        }
        return ret.toString(Charset.forName("MS949"))
    }

    /**
     * Reads a null-terminated string from the stream.
     *
     * @return The string read.
     */
    override fun readNullTerminatedASCIIString(): String {
        val bas = ByteArrayOutputStream()
        var b: Byte = 1
        while (b.toInt() != 0) {
            b = readByte()
            bas.write(b.toInt())
        }
        return bas.toString(Charset.forName("MS949"))
    }

    /**
     * Reads a Client convention lengthed ASCII string.
     * This consists of a short integer telling the length of the string,
     * then the string itself.
     *
     * @return The string read.
     */
    override fun readGameASCIIString(): String = readASCIIString(readShort().toInt())

    /**
     * Reads <code>num</code> bytes off the stream.
     *
     * @param num The number of bytes to read.
     * @return An array of bytes with the length of <code>num</code>
     */
    override fun read(num: Int): ByteArray {
        val ret = ByteArray(num)
        for (x in 0 until num) {
            ret[x] = readByte()
        }
        return ret
    }

    /**
     * Reads a Client Position information.
     * This consists of 2 short integer.
     *
     * @return The Position read.
     */
    override fun readPos(): Point {
        val x = readShort().toInt()
        val y = readShort().toInt()
        return Point(x, y)
    }

    /**
     * Skips the current position of the stream <code>num</code> bytes ahead.
     *
     * @param num Number of bytes to skip.
     */
    override fun skip(num: Int) {
        for (x in 0 until num) {
            readByte()
        }
    }

    override fun available(): Long = bs.available()

    override val bytesRead: Long
        get() = bs.bytesRead

    override fun toString(): String = bs.toString()
}