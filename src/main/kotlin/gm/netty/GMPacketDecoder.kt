package gm.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class GMPacketDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf, out: MutableList<Any>?) {
        out?.add(`in`.array())
    }
}