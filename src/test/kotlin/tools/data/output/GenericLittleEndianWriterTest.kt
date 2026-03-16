package tools.data.output

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Point
import java.io.ByteArrayOutputStream

class GenericLittleEndianWriterTest {

    private lateinit var baos: ByteArrayOutputStream
    private lateinit var writer: GenericLittleEndianWriter

    @BeforeEach
    fun setUp() {
        baos = ByteArrayOutputStream()
        writer = GenericLittleEndianWriter().apply {
            bos = BAOSByteOutputStream(baos)
        }
    }

    private fun bytes() = baos.toByteArray()

    // --- byte() ---

    @Test
    fun `write single byte`() {
        writer.byte(0x42.toByte())
        assertArrayEquals(byteArrayOf(0x42), bytes())
    }

    @Test
    fun `write byte as int`() {
        writer.byte(0xFF)
        assertArrayEquals(byteArrayOf(0xFF.toByte()), bytes())
    }

    @Test
    fun `write byte array`() {
        writer.byte(byteArrayOf(0x01, 0x02, 0x03))
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), bytes())
    }

    // --- skip() ---

    @Test
    fun `skip writes zero bytes`() {
        writer.skip(3)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00), bytes())
    }

    // --- short() ---

    @Test
    fun `write short in little endian order`() {
        writer.short(0x1234)
        assertArrayEquals(byteArrayOf(0x34, 0x12), bytes())
    }

    @Test
    fun `write short zero`() {
        writer.short(0)
        assertArrayEquals(byteArrayOf(0x00, 0x00), bytes())
    }

    // --- int() ---

    @Test
    fun `write int in little endian order`() {
        writer.int(0x12345678)
        assertArrayEquals(byteArrayOf(0x78, 0x56, 0x34, 0x12), bytes())
    }

    @Test
    fun `write int zero`() {
        writer.int(0)
        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x00), bytes())
    }

    // --- long() ---

    @Test
    fun `write long in little endian order`() {
        writer.long(0x0102030405060708L)
        assertArrayEquals(byteArrayOf(0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01), bytes())
    }

    @Test
    fun `write long zero`() {
        writer.long(0L)
        assertArrayEquals(ByteArray(8), bytes())
    }

    // --- ASCIIString() ---

    @Test
    fun `write ASCII string`() {
        writer.ASCIIString("Hi")
        assertArrayEquals(byteArrayOf(0x48, 0x69), bytes())
    }

    @Test
    fun `write ASCII string with max pads with zeros`() {
        writer.ASCIIString("Hi", 4)
        assertArrayEquals(byteArrayOf(0x48, 0x69, 0x00, 0x00), bytes())
    }

    @Test
    fun `write ASCII string with max equal to string length`() {
        writer.ASCIIString("Hi", 2)
        assertArrayEquals(byteArrayOf(0x48, 0x69), bytes())
    }

    // --- gameASCIIString() ---

    @Test
    fun `write game ASCII string prepends length as short`() {
        writer.gameASCIIString("Hi")
        // length = 2 as LE short, then "Hi"
        assertArrayEquals(byteArrayOf(0x02, 0x00, 0x48, 0x69), bytes())
    }

    // --- nullTerminatedASCIIString() ---

    @Test
    fun `write null terminated ASCII string appends zero byte`() {
        writer.nullTerminatedASCIIString("Hi")
        assertArrayEquals(byteArrayOf(0x48, 0x69, 0x00), bytes())
    }

    // --- pos() ---

    @Test
    fun `write point as two little endian shorts`() {
        writer.pos(Point(0x0102, 0x0304))
        assertArrayEquals(byteArrayOf(0x02, 0x01, 0x04, 0x03), bytes())
    }

    // --- bool() ---

    @Test
    fun `write bool true writes 1`() {
        writer.bool(true)
        assertArrayEquals(byteArrayOf(0x01), bytes())
    }

    @Test
    fun `write bool false writes 0`() {
        writer.bool(false)
        assertArrayEquals(byteArrayOf(0x00), bytes())
    }

    // --- bos null safety ---

    @Test
    fun `write with null bos does nothing`() {
        writer.bos = null
        writer.byte(0x01.toByte())
        assertArrayEquals(byteArrayOf(), bytes())
    }

}