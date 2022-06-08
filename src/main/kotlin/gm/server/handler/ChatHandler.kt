package gm.server.handler

import gm.GMPacketHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey
import net.server.Server
import tools.data.input.SeekableLittleEndianAccessor

class ChatHandler : GMPacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, session: ChannelHandlerContext?) {
        Server.gmChat(slea.readGameASCIIString(), session?.channel()?.attr(AttributeKey.valueOf<String>("NAME"))?.get().toString())
    }
}