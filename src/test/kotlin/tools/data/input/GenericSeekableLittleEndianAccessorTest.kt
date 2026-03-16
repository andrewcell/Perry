package tools.data.input

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GenericSeekableLittleEndianAccessorTest {

    private fun accessorOf(vararg bytes: Int): GenericSeekableLittleEndianAccessor {
        val byteArray = ByteArray(bytes.size) { bytes[it].toByte() }
        return GenericSeekableLittleEndianAccessor(ByteArrayByteStream(byteArray))
    }

    /**
     * A fake SeekableInputStreamByteStream whose seek() always throws,
     * allowing us to exercise the catch block in GenericSeekableLittleEndianAccessor.
     */
    private class ThrowingByteStream : SeekableInputStreamByteStream {
        override var position = 0
        override var bytesRead = 0L
        override fun readByte() = 0
        override fun available() = 0L
        override fun seek(offset: Long) = throw RuntimeException("forced seek failure")
    }

    // ─── position ────────────────────────────────────────────────────────────

    @Test
    fun `position is zero at start`() {
        assertEquals(0L, accessorOf(0x01, 0x02, 0x03).position)
    }

    @Test
    fun `position advances after read`() {
        val acc = accessorOf(0x01, 0x02, 0x03)
        acc.readByte()
        assertEquals(1L, acc.position)
        acc.readShort()
        assertEquals(3L, acc.position)
    }

    // ─── seek ────────────────────────────────────────────────────────────────

    @Test
    fun `seek moves position to given offset`() {
        val acc = accessorOf(0x00, 0x00, 0x42)
        acc.seek(2L)
        assertEquals(2L, acc.position)
    }

    @Test
    fun `seek then readByte returns byte at seeked position`() {
        val acc = accessorOf(0xAA, 0xBB, 0xCC)
        acc.seek(2L)
        assertEquals(0xCC.toByte(), acc.readByte())
    }

    @Test
    fun `seek to zero resets to start`() {
        val acc = accessorOf(0x11, 0x22, 0x33)
        acc.readByte()
        acc.readByte()
        acc.seek(0L)
        assertEquals(0L, acc.position)
        assertEquals(0x11.toByte(), acc.readByte())
    }

    @Test
    fun `seek does not affect bytesRead`() {
        val acc = accessorOf(0x01, 0x02, 0x03)
        acc.readByte()
        val readsBefore = acc.bytesRead
        acc.seek(0L)
        assertEquals(readsBefore, acc.bytesRead)
    }

    @Test
    fun `seek with invalid offset does not throw`() {
        // Exception should be caught and logged internally, not propagated
        val acc = accessorOf(0x01)
        assertDoesNotThrow { acc.seek(999L) }
    }

    @Test
    fun `seek swallows exception and does not propagate when backing stream throws`() {
        val acc = GenericSeekableLittleEndianAccessor(ThrowingByteStream())
        assertDoesNotThrow { acc.seek(5L) }
    }

    // ─── skip ────────────────────────────────────────────────────────────────

    @Test
    fun `skip advances position by num bytes`() {
        val acc = accessorOf(0x00, 0x00, 0x42)
        acc.skip(2)
        assertEquals(2L, acc.position)
    }

    @Test
    fun `skip then readByte returns correct byte`() {
        val acc = accessorOf(0x00, 0x00, 0x42)
        acc.skip(2)
        assertEquals(0x42.toByte(), acc.readByte())
    }

    @Test
    fun `skip zero does not change position`() {
        val acc = accessorOf(0x01, 0x02)
        acc.skip(0)
        assertEquals(0L, acc.position)
    }

    @Test
    fun `skip is additive from current position`() {
        val acc = accessorOf(0x01, 0x02, 0x03, 0x04)
        acc.skip(1)
        acc.skip(2)
        assertEquals(3L, acc.position)
    }

    @Test
    fun `skip uses seek rather than consuming bytes — bytesRead is not incremented`() {
        // Unlike the parent's skip (which calls readByte in a loop),
        // this override calls seek, so bytesRead should remain 0
        val acc = accessorOf(0x01, 0x02, 0x03)
        acc.skip(2)
        assertEquals(0L, acc.bytesRead)
    }
}