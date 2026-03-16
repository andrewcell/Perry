package tools.data.input

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class InputStreamByteStreamTest {

    private fun streamOf(vararg bytes: Int): InputStreamByteStream {
        val byteArray = ByteArray(bytes.size) { bytes[it].toByte() }
        return InputStreamByteStream(ByteArrayInputStream(byteArray))
    }

    // ─── readByte ────────────────────────────────────────────────────────────

    @Test
    fun `readByte returns correct unsigned byte values`() {
        val s = streamOf(0x00, 0x7F, 0xFF)
        assertEquals(0x00, s.readByte())
        assertEquals(0x7F, s.readByte())
        assertEquals(0xFF, s.readByte())
    }

    @Test
    fun `readByte increments bytesRead on each call`() {
        val s = streamOf(0x01, 0x02, 0x03)
        s.readByte()
        assertEquals(1L, s.bytesRead)
        s.readByte()
        s.readByte()
        assertEquals(3L, s.bytesRead)
    }

    @Test
    fun `readByte returns minus one and does not throw when stream throws`() {
        val throwing = InputStreamByteStream(object : InputStream() {
            override fun read() = throw RuntimeException("forced read failure")
        })
        assertDoesNotThrow {
            assertEquals(-1, throwing.readByte())
        }
    }

    @Test
    fun `readByte does not increment bytesRead when stream throws`() {
        val throwing = InputStreamByteStream(object : InputStream() {
            override fun read() = throw RuntimeException("forced read failure")
        })
        throwing.readByte()
        assertEquals(0L, throwing.bytesRead)
    }

    // ─── available ───────────────────────────────────────────────────────────

    @Test
    fun `available returns correct byte count`() {
        val s = streamOf(0x01, 0x02, 0x03)
        assertEquals(3L, s.available())
    }

    @Test
    fun `available decreases after read`() {
        val s = streamOf(0x01, 0x02)
        s.readByte()
        assertEquals(1L, s.available())
    }

    @Test
    fun `available returns zero and does not throw when stream throws`() {
        val throwing = InputStreamByteStream(object : InputStream() {
            override fun read() = 0
            override fun available() = throw RuntimeException("forced available failure")
        })
        assertDoesNotThrow {
            assertEquals(0L, throwing.available())
        }
    }

    // ─── bytesRead ───────────────────────────────────────────────────────────

    @Test
    fun `bytesRead is zero before any reads`() {
        assertEquals(0L, streamOf(0x01).bytesRead)
    }
}