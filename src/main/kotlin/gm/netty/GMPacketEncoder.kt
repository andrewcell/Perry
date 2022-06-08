package gm.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class GMPacketEncoder : MessageToByteEncoder<ByteArray>() {
    override fun encode(ctx: ChannelHandlerContext?, msg: ByteArray?, out: ByteBuf?) {
        out?.writeBytes(msg)
    }
}