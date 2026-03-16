package tools.data.output

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import net.SendPacketOpcode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import tools.ServerJSON
import tools.data.TestAppender
import tools.settings.Settings

class PacketLittleEndianWriterTest {

    private lateinit var testAppender: TestAppender
    private lateinit var logger: Logger

    @BeforeEach
    fun setUpLogger() {
        testAppender = TestAppender()
        testAppender.start()
        logger = LoggerFactory.getLogger(PacketLittleEndianWriter::class.java) as Logger
        logger.level = Level.TRACE
        logger.addAppender(testAppender)
    }

    @AfterEach
    fun tearDownLogger() {
        logger.detachAppender(testAppender)
    }

    private fun mockSettings(printSendPacket: Boolean) {
        mockkObject(ServerJSON)
        every { ServerJSON.settings } returns Settings(
            database = io.mockk.mockk(),
            worlds = emptyList(),
            printSendPacket = printSendPacket
        )
    }

    @AfterEach
    fun tearDownMock() {
        unmockkObject(ServerJSON)
    }

    @Test
    fun `bos is initialized on construction`() {
        val writer = PacketLittleEndianWriter()
        assert(writer.bos != null)
    }

    @Test
    fun `default buffer starts empty`() {
        val writer = PacketLittleEndianWriter()
        assertArrayEquals(byteArrayOf(), writer.getPacket())
    }

    @Test
    fun `custom size does not affect written content`() {
        val writer = PacketLittleEndianWriter(size = 64)
        writer.byte(0x01)
        assertArrayEquals(byteArrayOf(0x01), writer.getPacket())
    }

    @Test
    fun `getPacket returns written bytes`() {
        val writer = PacketLittleEndianWriter()
        writer.byte(0x01)
        writer.byte(0x02)
        writer.byte(0x03)
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), writer.getPacket())
    }

    @Test
    fun `getPacket is repeatable`() {
        val writer = PacketLittleEndianWriter()
        writer.int(0x12345678)
        val first = writer.getPacket()
        val second = writer.getPacket()
        assertArrayEquals(first, second)
    }

    @Test
    fun `opcode writes low byte of opcode value`() {
        val writer = PacketLittleEndianWriter()
        SendPacketOpcode.PING.value = 0x0A
        writer.opcode(SendPacketOpcode.PING)
        assertArrayEquals(byteArrayOf(0x0A), writer.getPacket())
    }

    @Test
    fun `opcode value is written as single byte`() {
        val writer = PacketLittleEndianWriter()
        SendPacketOpcode.LOGIN_STATUS.value = 0x00
        writer.opcode(SendPacketOpcode.LOGIN_STATUS)
        assertEquals(1, writer.getPacket().size)
    }

    @Test
    fun `getPacket with printSendPacket false does not log`() {
        mockSettings(printSendPacket = false)
        val writer = PacketLittleEndianWriter()
        writer.byte(0x01)
        writer.getPacket()
        assertTrue(testAppender.events.isEmpty())
    }

    @Test
    fun `getPacket with printSendPacket true logs trace with packet content`() {
        mockSettings(printSendPacket = true)
        val writer = PacketLittleEndianWriter()
        writer.byte(0x01)
        writer.getPacket()
        assertTrue(testAppender.events.isNotEmpty())
        assertEquals(Level.TRACE, testAppender.events.first().level)
        assertTrue(testAppender.events.first().formattedMessage.contains("Sending:"))
    }
}