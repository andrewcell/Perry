package tools.data.input

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Path

class RandomAccessByteStreamTest {

    @TempDir
    lateinit var tempDir: Path

    private fun rafOf(vararg bytes: Int): RandomAccessFile {
        val file = tempDir.resolve("test.bin").toFile()
        file.writeBytes(ByteArray(bytes.size) { bytes[it].toByte() })
        return RandomAccessFile(file, "r")
    }

    // ─── readByte ────────────────────────────────────────────────────────────

    @Test
    fun `readByte returns correct unsigned byte values in order`() {
        val s = RandomAccessByteStream(rafOf(0x00, 0x7F, 0xFF))
        assertEquals(0x00, s.readByte())
        assertEquals(0x7F, s.readByte())
        assertEquals(0xFF, s.readByte())
    }

    @Test
    fun `readByte returns minus one at end of file`() {
        val s = RandomAccessByteStream(rafOf(0x01))
        s.readByte() // consume only byte
        assertEquals(-1, s.readByte())
    }

    @Test
    fun `readByte does not increment bytesRead at end of file`() {
        val s = RandomAccessByteStream(rafOf(0x01))
        s.readByte()
        val before = s.bytesRead
        s.readByte() // EOF
        assertEquals(before, s.bytesRead)
    }

    @Test
    fun `readByte increments bytesRead on each successful read`() {
        val s = RandomAccessByteStream(rafOf(0x01, 0x02, 0x03))
        s.readByte()
        assertEquals(1L, s.bytesRead)
        s.readByte()
        s.readByte()
        assertEquals(3L, s.bytesRead)
    }

    @Test
    fun `readByte returns minus one and does not throw when IOException occurs`() {
        val closedRaf = rafOf(0x01).also { it.close() }
        val s = RandomAccessByteStream(closedRaf)
        assertDoesNotThrow {
            assertEquals(-1, s.readByte())
        }
    }

    @Test
    fun `readByte does not increment bytesRead when IOException occurs`() {
        val closedRaf = rafOf(0x01).also { it.close() }
        val s = RandomAccessByteStream(closedRaf)
        s.readByte()
        assertEquals(0L, s.bytesRead)
    }

    // ─── seek ────────────────────────────────────────────────────────────────

    @Test
    fun `seek moves position to given offset`() {
        val s = RandomAccessByteStream(rafOf(0xAA, 0xBB, 0xCC))
        s.seek(2L)
        assertEquals(2, s.position)
    }

    @Test
    fun `readByte after seek returns byte at seeked position`() {
        val s = RandomAccessByteStream(rafOf(0xAA, 0xBB, 0xCC))
        s.seek(1L)
        assertEquals(0xBB, s.readByte())
    }

    @Test
    fun `seek to zero resets to start`() {
        val s = RandomAccessByteStream(rafOf(0x11, 0x22))
        s.readByte()
        s.seek(0L)
        assertEquals(0, s.position)
        assertEquals(0x11, s.readByte())
    }

    // ─── position ────────────────────────────────────────────────────────────

    @Test
    fun `position is zero at start`() {
        assertEquals(0, RandomAccessByteStream(rafOf(0x01, 0x02)).position)
    }

    @Test
    fun `position advances after readByte`() {
        val s = RandomAccessByteStream(rafOf(0x01, 0x02, 0x03))
        s.readByte()
        assertEquals(1, s.position)
        s.readByte()
        assertEquals(2, s.position)
    }

    // ─── available ───────────────────────────────────────────────────────────

    @Test
    fun `available returns full file size at start`() {
        assertEquals(3L, RandomAccessByteStream(rafOf(0x01, 0x02, 0x03)).available())
    }

    @Test
    fun `available decreases after readByte`() {
        val s = RandomAccessByteStream(rafOf(0x01, 0x02, 0x03))
        s.readByte()
        assertEquals(2L, s.available())
    }

    @Test
    fun `available returns zero for empty file`() {
        assertEquals(0L, RandomAccessByteStream(rafOf()).available())
    }

    @Test
    fun `available returns zero and does not throw when IOException occurs`() {
        val closedRaf = rafOf(0x01, 0x02).also { it.close() }
        val s = RandomAccessByteStream(closedRaf)
        assertDoesNotThrow {
            assertEquals(0L, s.available())
        }
    }

    // ─── bytesRead ───────────────────────────────────────────────────────────

    @Test
    fun `bytesRead is zero before any reads`() {
        assertEquals(0L, RandomAccessByteStream(rafOf(0x01)).bytesRead)
    }
}