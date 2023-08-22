package net.server.channel

import client.Character
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import mu.KLoggable
import mu.KLogging
import net.ServerHandler
import net.netty.PacketDecoder
import net.netty.PacketEncoder
import net.server.PlayerStorage
import net.server.world.Party
import provider.DataProviderFactory
import scripting.event.EventScriptManager
import server.events.gm.Event
import server.maps.HiredMerchant
import server.maps.MapFactory
import tools.CoroutineManager
import tools.PacketCreator
import tools.ServerJSON.settings
import tools.packet.InteractPacket
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock

class Channel(val world: Int, val channelId: Int) : KLoggable {
    override val logger = logger()
    var port = 7575
    val players = PlayerStorage()
    var ip = ""
    var serverMessage = ""
        set(value) {
            field = value
            broadcastPacket(InteractPacket.serverMessage(message = value))
        }
    val mapFactory = MapFactory(DataProviderFactory.getDataProvider(File("${settings.wzPath}/Map.wz")), DataProviderFactory.getDataProvider(File("${settings.wzPath}/String.wz")), world, channelId)
    val hiredMerchants = mutableMapOf<Int, HiredMerchant>()
    private val merchantLock = ReentrantReadWriteLock(true)
    var event: Event? = null
    var finishedShutdown = false
    private val bootstrap = ServerBootstrap()
    val eventSM = EventScriptManager(this, settings.events)

    init {
        port = port + channelId - 1
        port += (world * 100)
        ip = "${settings.host}:$port"
        CoroutineManager.register(RespawnMaps(), 0, 7000)
        val bossGroup: EventLoopGroup = NioEventLoopGroup()
        val workerGroup: EventLoopGroup = NioEventLoopGroup()


        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel> () {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast("decoder", PacketDecoder())
                    ch.pipeline().addLast("encoder", PacketEncoder())
                    ch.pipeline().addLast("handler", ServerHandler(world, channelId))
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_SNDBUF, 4096 * 1024)
            .childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.bind(port).sync()
        eventSM.init()
        logger.info { "Channel $channelId from World $world is now opened at port $port" }
    }

    fun shutdown() {
        closeAllMerchants()
        players.disconnectAll()
        //session.unbind()
        finishedShutdown = true
        logger.info { "Channel $channelId in World $world is gone offline." }
    }

    private fun closeAllMerchants() {
        val wLock = merchantLock.writeLock()
        wLock.lock()
        hiredMerchants.values.forEach { it.forceClose() }
        hiredMerchants.clear()
        wLock.unlock()
    }

    fun addPlayer(chr: Character) {
        players.addPlayer(chr)
        chr.announce(InteractPacket.serverMessage(message = serverMessage)) // Multi world
    }

    fun removePlayer(chr: Character) = players.removePlayer(chr.id)

    fun getConnectedClients() = players.getAllCharacters().size

    fun broadcastPacket(data: ByteArray) = players.getAllCharacters().forEach { it.announce(data) }

    fun broadcastGMPacket(data: ByteArray) = players.getAllCharacters().forEach { if (it.isGM()) it.announce(data) }

    fun worldMessageYellow(m: String) = players.getAllCharacters().forEach { it.announce(PacketCreator.sendYellowTip(m)) }

    fun worldMessage(m: String) = players.getAllCharacters().forEach { it.dropMessage(message = m) }

    fun getPartyMembers(party: Party): List<Character> {
        val lst = mutableListOf<Character>()
        party.members.forEach { p ->
            if (p.channel == channelId) {
                val chr = p.name?.let { it1 -> players.getCharacterByName(it1) }
                chr?.let { lst.add(it) }
            }
        }
        return lst
    }

    fun addHiredMerchant(chrId: Int, h: HiredMerchant) {
        merchantLock.writeLock().lock()
        try {
            hiredMerchants[chrId] = h
        } finally {
            merchantLock.writeLock().unlock()
        }
    }

    fun removeHiredMerchant(chrId: Int) {
        merchantLock.writeLock().lock()
        try {
            hiredMerchants.remove(chrId)
        } finally {
            merchantLock.writeLock().unlock()
        }
    }

    fun multiBuddyFind(charIdFrom: Int, characterIds: List<Int>): List<Int> {
        return characterIds.filter {
            players.getCharacterById(it)?.buddyList?.containsVisible(charIdFrom) ?: false
        }
    }

    fun isConnected(name: String) = players.getCharacterByName(name) != null

    inner class RespawnMaps : Runnable {
        override fun run() {
            mapFactory.maps.values.forEach { it.respawn() }
        }
    }

    companion object : KLogging()
}