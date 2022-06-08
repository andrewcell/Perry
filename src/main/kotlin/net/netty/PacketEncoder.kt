package net.netty

import client.Client
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import mu.KLogging
import tools.ServerJSON

class PacketEncoder : MessageToByteEncoder<ByteArray>() {
    override fun encode(ctx: ChannelHandlerContext?, msg: ByteArray, out: ByteBuf) {
        val client = ctx?.channel()?.attr(Client.CLIENT_KEY)?.get()
        if (client != null) {
            val mutex = client.lock
            mutex.lock()
            try {
                if (ServerJSON.settings.modifiedClient) {
                    val unencrypted = ByteArray(msg.size)
                    System.arraycopy(msg, 0, unencrypted, 0, msg.size)
                    val ret = ByteArray(unencrypted.size + 4)
                    val header = client.sendCrypto.getPacketHeader(unencrypted.size)
                    client.sendCrypto.encrypt(unencrypted)
                    synchronized(client.sendCrypto) {
                        System.arraycopy(header, 0, ret, 0, 4)
                        System.arraycopy(unencrypted, 0, ret, 4, unencrypted.size)
                        out.writeBytes(ret)
                        ctx.flush()
                    }
                } else {
                    with(client.sendCrypto) {
                        out.writeBytes(getPacketHeader(msg.size))
                        out.writeBytes(encrypt(msg))
                        ctx.flush()
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Encode error." }
            } finally {
                mutex.unlock()
            }
        } else {
            out.writeBytes(msg)
        }
    }

    companion object : KLogging()
}