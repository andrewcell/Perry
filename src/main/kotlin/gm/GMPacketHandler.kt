package gm

import io.netty.channel.ChannelHandlerContext
import tools.data.input.SeekableLittleEndianAccessor

interface GMPacketHandler {
    fun handlePacket(slea: SeekableLittleEndianAccessor, session: ChannelHandlerContext?)
}