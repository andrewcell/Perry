package gm.server

import gm.GMPacketCreator
import gm.GMServerHandler
import gm.netty.GMPacketDecoder
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import mu.KLoggable
import net.server.Server

object GMServer : KLoggable {
    override val logger = logger()
    private val bootstrap = ServerBootstrap()
    val outGame = mutableMapOf<String, Channel>()
    val inGame = mutableMapOf<String, Channel>()
    var started = false
    const val KEYWORD = "Perry"

    fun startGMServer() {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                @Throws(java.lang.Exception::class)
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast("decoder", GMPacketDecoder())
                    ch.pipeline().addLast("encoder", gm.netty.GMPacketEncoder())
                    ch.pipeline().addLast("handler", GMServerHandler())
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_SNDBUF, 4096 * 1024)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
        try {
            bootstrap.bind(5252).sync()
            logger.info { "GM Server is now online. Listening port: 5252" }
        } catch (e: Exception) {
            logger.error(e) { "GM Server failed to start." }
        }
        Server.worlds.forEach { w ->
            w.channels.forEach { ch ->
                ch.players.getAllCharacters().forEach { chr ->
                    if (chr.isGM()) {
                        chr.client.session.let { inGame.put(chr.name, it) }
                    }
                }
            }
        }
        started = true
    }

    fun broadcastOutGame(packet: ByteArray, exclude: String?) {
        outGame.forEach { (_, ss) ->
            //if (ss.getAttribute("NAME") != exclude) {
                ss.write(packet)
            //}
        }
    }

    fun broadcastInGame(packet: ByteArray) {
        inGame.forEach { (_, ss) -> ss.write(packet) }
    }

    fun addInGame(name: String, session: Channel) {
        if (!inGame.containsKey(name)) {
            broadcastOutGame(GMPacketCreator.chat("$name has logged in."), null)
            broadcastOutGame(GMPacketCreator.addUser(name), null)
        }
        inGame[name] = session
    }

    fun addOutGame(name: String, session: Channel) = outGame.put(name, session)

    fun removeInGame(name: String) {
        if (inGame.remove(name) != null) {
            broadcastOutGame(GMPacketCreator.removeUser(name), null)
            broadcastOutGame(GMPacketCreator.chat("$name has logged out."), null)
        }
    }

    fun removeOutGame(name: String) {
        val ss = outGame.remove(name)
        if (ss != null) {
            if (ss.isOpen) {
                broadcastOutGame(GMPacketCreator.removeUser(name), null)
                broadcastOutGame(GMPacketCreator.chat("$name has logged out."), null)
            }
        }
    }


    operator fun contains(name: String?): Boolean {
        return inGame.containsKey(name) || outGame.containsKey(name)
    }

    private fun closeAllSessions() {
        try {
            val sss = outGame.values.toMutableList()
            synchronized(sss) {
                val outIt = sss.iterator()
                while (outIt.hasNext()) {
                    outIt.next().close()
                    outIt.remove()
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to close all sessions in GM server." }
        }
    }

    fun getUserList(exclude: String?): List<String> {
        val returnList = outGame.keys.toMutableList()
        returnList.remove(exclude) //Already sent in LoginHandler (So you are first on the list (:
        returnList.addAll(inGame.keys)
        return returnList.toList()
    }

    fun shutdown() { //nothing to save o.o
        try {
            closeAllSessions()
            //acceptor.unbind()
            logger.info { "GM server is now offline." }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}