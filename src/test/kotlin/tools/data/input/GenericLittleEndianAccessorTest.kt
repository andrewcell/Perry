package tools.data.input

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.Point
import java.lang.Double.longBitsToDouble
import java.lang.Float.intBitsToFloat

class GenericLittleEndianAccessorTest {

    private fun streamOf(vararg bytes: Int): GenericLittleEndianAccessor {
        val byteArray = ByteArray(bytes.size) { bytes[it].toByte() }
        return GenericLittleEndianAccessor(ByteArrayByteStream(byteArray))
    }

    // ─── readByte ────────────────────────────────────────────────────────────

    @Test
    fun `readByte returns correct signed byte`() {
        val acc = streamOf(0xFF, 0x01)
        assertEquals(0xFF.toByte(), acc.readByte())
        assertEquals(0x01.toByte(), acc.readByte())
    }

    // ─── readShort ───────────────────────────────────────────────────────────

    @Test
    fun `readShort reads little-endian short`() {
        // 0x01 0x02 → 0x0201 = 513
        val acc = streamOf(0x01, 0x02)
        assertEquals(0x0201.toShort(), acc.readShort())
    }

    @Test
    fun `readShort reads zero correctly`() {
        assertEquals(0.toShort(), streamOf(0x00, 0x00).readShort())
    }

    @Test
    fun `readShort reads max positive short`() {
        // 0xFF 0x7F → 0x7FFF = 32767
        assertEquals(Short.MAX_VALUE, streamOf(0xFF, 0x7F).readShort())
    }

    // ─── readInt ─────────────────────────────────────────────────────────────

    @Test
    fun `readInt reads little-endian int`() {
        // 0x01 0x02 0x03 0x04 → 0x04030201 = 67305985
        val acc = streamOf(0x01, 0x02, 0x03, 0x04)
        assertEquals(0x04030201, acc.readInt())
    }

    @Test
    fun `readInt reads zero`() {
        assertEquals(0, streamOf(0x00, 0x00, 0x00, 0x00).readInt())
    }

    @Test
    fun `readInt reads Int MAX_VALUE`() {
        // Int.MAX_VALUE = 0x7FFFFFFF, little-endian: FF FF FF 7F
        assertEquals(Int.MAX_VALUE, streamOf(0xFF, 0xFF, 0xFF, 0x7F).readInt())
    }

    // ─── readLong ────────────────────────────────────────────────────────────

    @Test
    fun `readLong reads little-endian long`() {
        // 1L little-endian = 01 00 00 00 00 00 00 00
        val acc = streamOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        assertEquals(1L, acc.readLong())
    }

    @Test
    fun `readLong reads Long MAX_VALUE`() {
        // Long.MAX_VALUE = 0x7FFFFFFFFFFFFFFF, LE: FF FF FF FF FF FF FF 7F
        val acc = streamOf(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x7F)
        assertEquals(Long.MAX_VALUE, acc.readLong())
    }

    // ─── readFloat ───────────────────────────────────────────────────────────

    @Test
    fun `readFloat reads IEEE 754 float`() {
        val expected = 1.5f
        val bits = java.lang.Float.floatToIntBits(expected)
        val acc = streamOf(
            bits and 0xFF,
            (bits shr 8) and 0xFF,
            (bits shr 16) and 0xFF,
            (bits shr 24) and 0xFF
        )
        assertEquals(expected, acc.readFloat())
    }

    // ─── readDouble ──────────────────────────────────────────────────────────

    @Test
    fun `readDouble reads IEEE 754 double`() {
        val expected = 3.14
        val bits = java.lang.Double.doubleToLongBits(expected)
        val acc = streamOf(
            (bits and 0xFF).toInt(),
            ((bits shr 8) and 0xFF).toInt(),
            ((bits shr 16) and 0xFF).toInt(),
            ((bits shr 24) and 0xFF).toInt(),
            ((bits shr 32) and 0xFF).toInt(),
            ((bits shr 40) and 0xFF).toInt(),
            ((bits shr 48) and 0xFF).toInt(),
            ((bits shr 56) and 0xFF).toInt()
        )
        assertEquals(expected, acc.readDouble())
    }

    // ─── readChar ────────────────────────────────────────────────────────────

    @Test
    fun `readChar reads character as little-endian short`() {
        // 'A' = 0x0041, LE: 0x41 0x00
        val acc = streamOf(0x41, 0x00)
        assertEquals('A', acc.readChar())
    }

    // ─── readASCIIString ─────────────────────────────────────────────────────

    @Test
    fun `readASCIIString reads n bytes as string`() {
        val acc = streamOf(0x48, 0x69) // "Hi" in ASCII
        assertEquals("Hi", acc.readASCIIString(2))
    }

    @Test
    fun `readASCIIString with zero length returns empty string`() {
        val acc = streamOf(0x41)
        assertEquals("", acc.readASCIIString(0))
    }

    // ─── readNullTerminatedASCIIString ───────────────────────────────────────

    @Test
    fun `readNullTerminatedASCIIString reads until null byte`() {
        // "OK\0"
        val acc = streamOf(0x4F, 0x4B, 0x00)
        assertEquals("OK", acc.readNullTerminatedASCIIString().trimEnd('\u0000'))
    }

    // ─── readGameASCIIString ─────────────────────────────────────────────────

    @Test
    fun `readGameASCIIString reads length-prefixed string`() {
        // length = 2 (LE short: 02 00), then "Hi"
        val acc = streamOf(0x02, 0x00, 0x48, 0x69)
        assertEquals("Hi", acc.readGameASCIIString())
    }

    // ─── read(num) ───────────────────────────────────────────────────────────

    @Test
    fun `read returns correct byte array`() {
        val acc = streamOf(0x0A, 0x0B, 0x0C)
        assertArrayEquals(byteArrayOf(0x0A, 0x0B, 0x0C), acc.read(3))
    }

    @Test
    fun `read with zero returns empty array`() {
        val acc = streamOf(0x01, 0x02)
        assertArrayEquals(byteArrayOf(), acc.read(0))
    }

    // ─── readPos ─────────────────────────────────────────────────────────────

    @Test
    fun `readPos reads two little-endian shorts as x and y`() {
        // x = 0x0064 = 100, y = 0x00C8 = 200 (LE: 64 00 C8 00)
        val acc = streamOf(0x64, 0x00, 0xC8, 0x00)
        assertEquals(Point(100, 200), acc.readPos())
    }

    @Test
    fun `readPos reads negative coordinates`() {
        // x = -1 (0xFFFF LE: FF FF), y = -2 (0xFFFE LE: FE FF)
        val acc = streamOf(0xFF, 0xFF, 0xFE, 0xFF)
        assertEquals(Point(-1, -2), acc.readPos())
    }

    // ─── skip ────────────────────────────────────────────────────────────────

    @Test
    fun `skip advances position by num bytes`() {
        val acc = streamOf(0x00, 0x00, 0x42)
        acc.skip(2)
        assertEquals(0x42.toByte(), acc.readByte())
    }

    @Test
    fun `skip zero does not advance position`() {
        val acc = streamOf(0x42)
        acc.skip(0)
        assertEquals(0x42.toByte(), acc.readByte())
    }

    // ─── available / bytesRead / toString ───────────────────────────────────

    @Test
    fun `available delegates to backing stream`() {
        val acc = streamOf(0x01, 0x02, 0x03)
        assertEquals(3L, acc.available())
        acc.readByte()
        assertEquals(2L, acc.available())
    }

    @Test
    fun `bytesRead reflects total bytes consumed`() {
        val acc = streamOf(0x01, 0x02, 0x03, 0x04)
        acc.readShort()   // 2 bytes
        acc.readByte()    // 1 byte
        assertEquals(3L, acc.bytesRead)
    }

    @Test
    fun `toString delegates to backing stream`() {
        val bs = ByteArrayByteStream(byteArrayOf(0x01))
        val acc = GenericLittleEndianAccessor(bs)
        assertEquals(bs.toString(), acc.toString())
    }
}