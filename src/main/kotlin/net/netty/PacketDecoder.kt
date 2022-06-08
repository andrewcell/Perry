package net.netty

import client.Client
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import tools.KMSEncryption2
import tools.data.input.ByteArrayByteStream
import tools.data.input.GenericSeekableLittleEndianAccessor

class PacketDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext?, `in`: ByteBuf, out: MutableList<Any>) {
        val client = ctx?.channel()?.attr(Client.CLIENT_KEY)?.get() ?: return
        if (`in`.readableBytes() < 4) return
        val packetHeader = `in`.readInt()
        val packetLength = KMSEncryption2.getPacketLength(packetHeader)
        if (`in`.readableBytes() < packetLength) {
            `in`.resetReaderIndex()
            return
        }
        `in`.markReaderIndex()
        val decoded = ByteArray(packetLength)
        `in`.readBytes(decoded)
        `in`.markReaderIndex()
        out.add(GenericSeekableLittleEndianAccessor(ByteArrayByteStream(client.receiveCrypto.decrypt(decoded))))
    }
}