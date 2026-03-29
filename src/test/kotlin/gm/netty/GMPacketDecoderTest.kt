package gm.netty

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class GMPacketDecoderTest {
//
//    @Test
//    fun `decode should add byte array from ByteBuf to out list`() {
//        // Arrange
//        val decoder = GMPacketDecoder()
//        val ctx = mockk<ChannelHandlerContext>()
//        val buf = mockk<ByteBuf>()
//        val testData = "test data".toByteArray(StandardCharsets.UTF_8)
//        every { buf.array() } returns testData
//
//        val out = mutableListOf<Any>()
//
//        // Act
//        decoder.decode(ctx, buf, out)
//
//        // Assert
//        verify(exactly = 1) { buf.array() }
//        assert(out.size == 1)
//        assert(out[0] === testData)
//    }
//
//    @Test
//    fun `decode should handle null in gracefully`() {
//        val decoder = GMPacketDecoder()
//        val ctx = mockk<ChannelHandlerContext>()
//        val out = mutableListOf<Any>()
//
//        // Act & Assert (no crash, no NPE)
//        decoder.decode(ctx, null as ByteBuf?, out)
//    }
//
//    @Test
//    fun `decode should handle empty out list`() {
//        val decoder = GMPacketDecoder()
//        val ctx = mockk<ChannelHandlerContext>()
//        val buf = mockk<ByteBuf>()
//        every { buf.array() } returns "data".toByteArray()
//
//        val out = mutableListOf<Any>()
//
//        decoder.decode(ctx, buf, out)
//
//        verify(exactly = 1) { buf.array() }
//        assert(out.size == 1)
//    }
//
//    @Test
//    fun `decode should not add anything when out is null`() {
//        val decoder = GMPacketDecoder()
//        val ctx = mockk<ChannelHandlerContext>()
//        val buf = mockk<ByteBuf>()
//
//        // Act & Assert: no exception, and nothing added (since out is null)
//        decoder.decode(ctx, buf, null)
//    }
}