package gm

import gm.server.GMServer
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.AttributeKey
import tools.data.input.GenericSeekableLittleEndianAccessor

class GMServerHandler : SimpleChannelInboundHandler<GenericSeekableLittleEndianAccessor>() {
    private val processor = GMPacketProcessor()

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        GMServer.removeOutGame(ctx?.channel()?.attr(AttributeKey.valueOf<String>("NAME")).toString())
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        ctx?.let {
            synchronized(it) {
                GMServer.removeOutGame(it.channel().attr(AttributeKey.valueOf<String>("NAME")).get())
            }
            super.channelInactive(ctx)
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: GenericSeekableLittleEndianAccessor) {
        val opcode = msg.readShort()
        val packetHandler = processor.getHandler(opcode)
        packetHandler?.handlePacket(msg, ctx)
    }
}