package net.netty

import client.Client
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import tools.ServerJSON

/**
 * Netty encoder that transforms outgoing byte array packets into encrypted network data.
 *
 * This encoder handles the encryption and framing of outgoing packets before they are
 * sent to the game client. It supports both standard and modified client encryption modes
 * based on server configuration.
 *
 * The encoding process includes:
 * - Generating a packet header with size information
 * - Encrypting the packet payload using the client's send cipher
 * - Writing the complete framed packet to the output buffer
 *
 * If no client is associated with the channel (e.g., during initial handshake),
 * the raw bytes are written directly without encryption.
 */
class PacketEncoder : MessageToByteEncoder<ByteArray>() {
    private val logger = KotlinLogging.logger {  }

    /**
     * Encodes an outgoing packet by encrypting it and adding the appropriate header.
     *
     * @param ctx The channel handler context, used to retrieve the associated client
     * @param msg The raw packet data to be encoded and sent
     * @param out The output buffer where the encoded packet will be written
     */
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
}