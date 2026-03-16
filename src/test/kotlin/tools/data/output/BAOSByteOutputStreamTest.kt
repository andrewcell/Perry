package tools.data.output

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class BAOSByteOutputStreamTest {

    private lateinit var baos: ByteArrayOutputStream
    private lateinit var stream: BAOSByteOutputStream

    @BeforeEach
    fun setUp() {
        baos = ByteArrayOutputStream()
        stream = BAOSByteOutputStream(baos)
    }

    @Test
    fun `write single byte is stored in underlying stream`() {
        stream.writeByte(0x42)
        assertArrayEquals(byteArrayOf(0x42), baos.toByteArray())
    }

    @Test
    fun `write multiple bytes are stored in order`() {
        stream.writeByte(0x01)
        stream.writeByte(0x02)
        stream.writeByte(0x03)
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), baos.toByteArray())
    }

    @Test
    fun `write 0xFF byte is preserved correctly`() {
        stream.writeByte(0xFF.toByte())
        assertArrayEquals(byteArrayOf(0xFF.toByte()), baos.toByteArray())
    }

    @Test
    fun `write zero byte is stored`() {
        stream.writeByte(0x00)
        assertArrayEquals(byteArrayOf(0x00), baos.toByteArray())
    }
}