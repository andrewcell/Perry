package tools.data.input

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ByteArrayByteStreamTest {

    // ─── construction ────────────────────────────────────────────────────────

    @Test
    fun `initial position is zero`() {
        val stream = ByteArrayByteStream(byteArrayOf(1, 2, 3))
        assertEquals(0, stream.position)
    }

    @Test
    fun `initial bytesRead is zero`() {
        val stream = ByteArrayByteStream(byteArrayOf(1, 2, 3))
        assertEquals(0L, stream.bytesRead)
    }

    // ─── available ───────────────────────────────────────────────────────────

    @Test
    fun `available returns full array size at start`() {
        val stream = ByteArrayByteStream(byteArrayOf(10, 20, 30))
        assertEquals(3L, stream.available())
    }

    @Test
    fun `available returns zero for empty array`() {
        val stream = ByteArrayByteStream(byteArrayOf())
        assertEquals(0L, stream.available())
    }

    @Test
    fun `available decreases after each read`() {
        val stream = ByteArrayByteStream(byteArrayOf(1, 2, 3))
        stream.readByte()
        assertEquals(2L, stream.available())
        stream.readByte()
        assertEquals(1L, stream.available())
    }

    // ─── readByte ────────────────────────────────────────────────────────────

    @Test
    fun `readByte returns correct byte values in order`() {
        val stream = ByteArrayByteStream(byteArrayOf(0x0A, 0x1B, 0x2C))
        assertEquals(0x0A, stream.readByte())
        assertEquals(0x1B, stream.readByte())
        assertEquals(0x2C, stream.readByte())
    }

    @Test
    fun `readByte treats byte as unsigned (0 to 255)`() {
        // 0xFF as a signed byte is -1; readByte should return 255
        val stream = ByteArrayByteStream(byteArrayOf(0xFF.toByte(), 0x80.toByte()))
        assertEquals(255, stream.readByte())
        assertEquals(128, stream.readByte())
    }

    @Test
    fun `readByte increments position`() {
        val stream = ByteArrayByteStream(byteArrayOf(1, 2, 3))
        stream.readByte()
        assertEquals(1, stream.position)
        stream.readByte()
        assertEquals(2, stream.position)
    }

    @Test
    fun `readByte increments bytesRead`() {
        val stream = ByteArrayByteStream(byteArrayOf(1, 2, 3))
        stream.readByte()
        assertEquals(1L, stream.bytesRead)
        stream.readByte()
        assertEquals(2L, stream.bytesRead)
    }

    // ─── seek ────────────────────────────────────────────────────────────────

    @Test
    fun `seek moves position to specified offset`() {
        val stream = ByteArrayByteStream(byteArrayOf(10, 20, 30, 40))
        stream.seek(2L)
        assertEquals(2, stream.position)
    }

    @Test
    fun `seek to zero resets position to start`() {
        val stream = ByteArrayByteStream(byteArrayOf(1, 2, 3))
        stream.readByte()
        stream.readByte()
        stream.seek(0L)
        assertEquals(0, stream.position)
    }

    @Test
    fun `readByte after seek returns byte at new position`() {
        val stream = ByteArrayByteStream(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()))
        stream.seek(2L)
        assertEquals(0xCC, stream.readByte())
    }

    @Test
    fun `seek does not affect bytesRead`() {
        val stream = ByteArrayByteStream(byteArrayOf(1, 2, 3))
        stream.readByte()
        val readsBefore = stream.bytesRead
        stream.seek(0L)
        assertEquals(readsBefore, stream.bytesRead)
    }

    // ─── toString ────────────────────────────────────────────────────────────

    @Test
    fun `toString contains hex of full array`() {
        val stream = ByteArrayByteStream(byteArrayOf(0xDE.toByte(), 0xAD.toByte()))
        val str = stream.toString()
        assertTrue(str.contains("DE"), "Expected DE in: $str")
        assertTrue(str.contains("AD"), "Expected AD in: $str")
    }

    @Test
    fun `toString shows remaining bytes from current position`() {
        val stream = ByteArrayByteStream(byteArrayOf(0x01, 0x02, 0x03))
        stream.readByte() // consume first byte
        val str = stream.toString()
        // "Now" section should contain 02 and 03, but not 01 (already consumed)
        val nowSection = str.substringAfter("Now: ")
        assertTrue(nowSection.contains("02"), "Expected 02 in remaining: $nowSection")
        assertTrue(nowSection.contains("03"), "Expected 03 in remaining: $nowSection")
        assertFalse(nowSection.contains("01"), "Did not expect 01 in remaining: $nowSection")
    }

    @Test
    fun `toString shows END OF STRING when stream is exhausted`() {
        val stream = ByteArrayByteStream(byteArrayOf(0x01))
        stream.readByte()
        assertTrue(stream.toString().contains("END OF STRING"))
    }
}