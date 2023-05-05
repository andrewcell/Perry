package net

import client.Client
import constants.ServerConstants
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.runBlocking
import mu.KLogging
import net.server.Server
import tools.*
import tools.data.input.ByteArrayByteStream
import tools.data.input.GenericSeekableLittleEndianAccessor
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.LoginPacket
import java.io.IOException
import java.util.*
import kotlin.random.Random

class ServerHandler(val world: Int = -1, val channel: Int = -1) : SimpleChannelInboundHandler<SeekableLittleEndianAccessor>() {
    private val processor = PacketProcessor.getProcessor(world, channel)

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        if (cause is IOException || cause is ClassCastException) return
        val mc = ctx?.channel()?.attr(Client.CLIENT_KEY)?.get()
        if (mc?.player != null) {
            logger.warn(cause) { "Exception miss handled or caught by ${mc.player?.name}" }
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        if (!Server.online) {
            ctx?.close()
        }
        /*if (ctx != null) {
            logger.info { "${ctx.channel().remoteAddress()} is connected." }
        }*/
        val ivReceive = arrayOf(70, 13, 122, Random.nextInt(255).toByte()).toByteArray()
        val ivSend = arrayOf(82, 53, 120, Random.nextInt(255).toByte()).toByteArray()
        val sendCipher = if (ServerJSON.settings.modifiedClient) {
            KMSEncryption2(ivSend, (0xFFFF - ServerConstants.gameVersion).toShort())
        } else KMSEncryption(ivSend, ((0xFFFF - ServerConstants.gameVersion).toShort()))
        val receiveCipher = if (ServerJSON.settings.modifiedClient) {
            KMSEncryption2(ivReceive, ServerConstants.gameVersion)
        } else KMSEncryption(ivReceive, ServerConstants.gameVersion)
        val client = Client(sendCipher, receiveCipher, ctx?.channel()!!)
        client.world = world
        client.channel = channel
        ctx.writeAndFlush(LoginPacket.getHello(ServerConstants.gameVersion, ivSend, ivReceive))
        ctx.channel()?.attr(Client.CLIENT_KEY)?.set(client)
        if (world != -1 && channel != -1) {
            logger.info { "Client(${ctx.channel().remoteAddress()}) connected to Channel server. World: $world, Channel: $channel" }
        } else {
            logger.info { "Client(${ctx.channel().remoteAddress()}) connected to Login server." }
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        val client: Client? = ctx!!.channel().attr(Client.CLIENT_KEY).get()

        if (client != null) {
            try {
                val inCashShop = client.player?.cashShop?.opened ?: false
                client.disconnect(false, inCashShop)
            } catch (t: Throwable) {
                logger.error(t) { "Account Stuck."}
            } finally {
                ctx.close()
                ctx.channel().attr(Client.CLIENT_KEY).set(null)
            }
            logger.debug { "${client.getSessionIPAddress()} disconnected server." }
        }

        if (client == null) { }

        ctx.channel().attr(Client.CLIENT_KEY).set(null)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: SeekableLittleEndianAccessor) {
        if (ServerJSON.settings.printReceivePacket) {
            msg as GenericSeekableLittleEndianAccessor
            val arr = (msg.bs as ByteArrayByteStream).array
            logger.trace { """
                 Receiving:
                 ${HexTool.toString(arr)}
                 ${HexTool.toStringFromASCII(arr)}""".trimIndent()
            }
        }
        //val slea = GenericSeekableLittleEndianAccessor(ByteArrayByteStream(content))
        val packetId = msg.readByte().toInt() and 0xFF
        if (packetId < 0) return
        val client = ctx?.channel()?.attr(Client.CLIENT_KEY)?.get() ?: return
        client.setTimesByPacketId(packetId)
        val packetHandler = processor.getHandler(packetId.toShort())
        if (packetHandler != null && packetHandler.validateState(client)) {
            try {
                logger.debug { "Calling packet handler ${packetHandler.javaClass.simpleName}. Opcode: $packetId" }
                runBlocking {
                    packetHandler.handlePacket(msg, client)
                }
            } catch (t: Exception) {
                logger.error(t) { "Exception missed when handling packet." }
                ctx.write(PacketCreator.enableActions())
            }
        }
    }

    companion object : KLogging() {
        fun initiate() {
            val blocked = EnumSet.noneOf(RecvPacketOpcode::class.java)
            val sBlocked = EnumSet.noneOf(RecvPacketOpcode::class.java)
            val block = arrayOf(
                RecvPacketOpcode.NPC_ACTION,
                RecvPacketOpcode.MOVE_PLAYER,
                RecvPacketOpcode.PONG,
                RecvPacketOpcode.MOVE_PET,
                RecvPacketOpcode.MOVE_SUMMON,
                RecvPacketOpcode.MOVE_LIFE,
                RecvPacketOpcode.HEAL_OVER_TIME,
                RecvPacketOpcode.STRANGE_DATA,
                RecvPacketOpcode.AUTO_AGGRO,
                RecvPacketOpcode.CANCEL_DEBUFF
            )
            val serverBlock = arrayOf(
                RecvPacketOpcode.CHANGE_KEYMAP,
                RecvPacketOpcode.ITEM_PICKUP,
                RecvPacketOpcode.PET_LOOT,
                RecvPacketOpcode.TAKE_DAMAGE,
                RecvPacketOpcode.FACE_EXPRESSION,
                RecvPacketOpcode.USE_ITEM,
                RecvPacketOpcode.CLOSE_RANGE_ATTACK,
                RecvPacketOpcode.MAGIC_ATTACK,
                RecvPacketOpcode.RANGED_ATTACK,
                RecvPacketOpcode.SPECIAL_MOVE,
                RecvPacketOpcode.GENERAL_CHAT,
                RecvPacketOpcode.MONSTER_BOMB,
                RecvPacketOpcode.PET_AUTO_POT,
                RecvPacketOpcode.USE_CASH_ITEM,
                RecvPacketOpcode.PARTYCHAT,
                RecvPacketOpcode.CANCEL_BUFF,
                RecvPacketOpcode.SKILL_EFFECT,
                RecvPacketOpcode.CHAR_INFO_REQUEST,
                RecvPacketOpcode.DISTRIBUTE_AP,
                RecvPacketOpcode.SPAWN_PET,
                RecvPacketOpcode.SUMMON_ATTACK,
                RecvPacketOpcode.ITEM_MOVE
            )
            blocked.addAll(listOf(*block))
            sBlocked.addAll(listOf(*serverBlock))
        }
    }
}