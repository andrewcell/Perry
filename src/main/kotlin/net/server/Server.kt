package net.server

import client.SkillFactory
import constants.ServerConstants
import database.*
import gm.GMPacketCreator
import gm.server.GMServer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.timeout.IdleStateHandler
import kotlinx.coroutines.Runnable
import mu.KLoggable
import net.ServerHandler
import net.netty.PacketDecoder
import net.netty.PacketEncoder
import net.server.channel.Channel
import net.server.guild.Guild
import net.server.guild.GuildCharacter
import net.server.world.World
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import server.CashShop
import server.ItemInformationProvider
import server.life.MonsterInformationProvider
import server.maps.MapFactory
import tools.CoroutineManager
import tools.ServerJSON.settings
import tools.packet.InteractPacket
import webapi.WebApiApplication
import java.util.*
import kotlin.system.exitProcess

object Server : Runnable, KLoggable {
    override val logger = logger()
    val channels = mutableListOf<MutableMap<Int, String>>()
    val worlds = mutableListOf<World>()
    private val worldRecommendedList = mutableListOf<Pair<Int, Boolean>>()
    private val guilds = mutableMapOf<Int, Guild>()
    val buffStorage = PlayerBuffStorage()
    val subnetInfo = Properties()
    var online = false
    private val startTime = System.currentTimeMillis()

    fun removeChannel(worldId: Int, channel: Int) {
        channels.removeAt(channel)
        worlds[worldId].removeChannel(channel)
    }

    fun getChannel(world: Int, channel: Int) = worlds[world].getChannel(channel)

    fun getChannelsFromWorld(world: Int) = worlds[world].channels

    fun getAllChannels(): List<Channel> {
        val list = mutableListOf<Channel>()
        worlds.forEach { list += it.channels }
        return list.toList()
    }

    fun getGuild(id: Int) = guilds[id]

    fun getGuild(mgc: GuildCharacter) = getGuild(mgc.guildId)

    fun getGuild(id: Int, mgc: GuildCharacter): Guild? {
        synchronized(guilds) {
            val guild = guilds[id]
            guild?.let { return it }
            val g = Guild(mgc)
            if (g.id == -1) return null
            guilds[id] = g
            return g
        }
    }

    fun setGuildMemberOnline(mgc: GuildCharacter, online: Boolean, channel: Int) {
        getGuild(mgc.guildId, mgc)?.setOnline(mgc.id, online, channel)
    }

    fun addGuildMember(mgc: GuildCharacter): Int {
        return guilds[mgc.guildId]?.addGuildMember(mgc) ?: return 0
    }

    fun deleteGuildCharacter(mgc: GuildCharacter) {
        setGuildMemberOnline(mgc, false, -1)
        val g = getGuild(mgc.guildId, mgc)
        if (mgc.guildRank > 1)
            g?.leaveGuild(mgc)
        else
            g?.disbandGuild()
    }

    fun getChannelServer(world: Int) = channels[world].keys

    fun getHighestChannelId() = channels[0].keys.maxOf { it }

    fun getIp(world: Int, channel: Int) = channels[world][channel]

    fun getWorld(world: Int) = worlds[world]

    private fun getElapsedTime(l: Long) = (l / 1000) % 60

    fun broadcastMessage(world: Int, packet: ByteArray) {
        getChannelsFromWorld(world).forEach { it.broadcastPacket(packet) }
    }

    private fun broadcastGMMessage(world: Int, packet: ByteArray) {
        getChannelsFromWorld(world).forEach { it.broadcastGMPacket(packet) }
    }

    private fun connectDatabase() {
        with(settings.database) {
            logger.debug { "Connecting to database server. $type://$username:***@$host:$port/$database" }
            val db: Database? = when (type.lowercase()) {
                "mysql" -> {
                    Database.connect(
                        "jdbc:mysql://$host:$port/$database",
                        "com.mysql.cj.jdbc.Driver",
                        user = username,
                        password = password
                    )
                }
                "mariadb" -> {
                    Database.connect(
                        "jdbc:mysql://$host:$port/$database?characterEncoding=utf8&useUnicode=true",
                        "org.mariadb.jdbc.Driver",
                        user = username,
                        password = password,
                    )
                }
                "sqlite" -> {
                    Database.connect("jdbc:sqlite:$database", driver = "org.sqlite.JDBC")
                }
                "mssql" -> {
                    Database.connect(
                        "jdbc:sqlserver://$host:$port;databaseName=$database",
                        driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver",
                        user = username,
                        password = password
                    )
                }
                "postgresql" -> {
                    Database.connect(
                        "jdbc:postgresql://$host:$port/$database",
                        driver = "org.postgresql.Driver",
                        user = username,
                        password = password
                    )
                }
                "h2" -> {
                    Database.connect(
                        "jdbc:h2://$database",
                        driver = "org.h2.Driver",
                        user = username,
                        password = password
                    )
                }
                "oracle" -> {
                    Database.connect(
                        "jdbc:oracle:thin:@//$host:$port/$database",
                        driver = "oracle.jdbc.OracleDriver",
                        user = username,
                        password = password
                    )
                }
                else -> {
                    logger.error { "Invalid database type or driver. Aborting." }
                    exitProcess(1)
                }
            }
            db?.let { logger.info { "Connected to ${it.vendor} - ${it.version}" } }
        }
    }

    override fun run() {
        val timeToTake = System.currentTimeMillis()
        connectDatabase()
        transaction {
            addLogger(Slf4jSqlDebugLogger)
            if (settings.database.createTableAtStart) {
                try {
                    SchemaUtils.createMissingTablesAndColumns(
                        Accounts, AreaInfos, BBSReplies, BBSThreads,
                        Buddies, Characters, CoolDowns, CustomQuests, DueyItems,
                        DueyPackages, EventStats, FameLog, Gifts, GMLog, Guilds,
                        HiredMerchants, HtSquads, InventoryEquipment, InventoryItems,
                        IPBans, IPLogs, KeyMap, KeyValues, MacBans, MacFilters, MonsterBooks,
                        Notes, NXCodes, Pets, PlayerNpcs, PlayerNpcsEquip, QuestActions,
                        QuestProgress, QuestRequirements, QuestStatuses, Reports, Responses,
                        SavedLocations, Skills, SpecialCashItems, Storages, TrockLocations, Wishlists
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Failed to create missing tables." }
                    exitProcess(1)
                }
            }
            Runtime.getRuntime().addShutdownHook(Thread(shutdown(false)))
            if (settings.webApi.enable) WebApiApplication.main()
            try {
                Accounts.update { it[loggedIn] = 0 }
                Characters.update { it[hasMerchant] = false }
            } catch (e: Exception) {
                logger.error(e) { "Failed to reset logged in info." }
                //log("Failed to reset logged in info. ${e.message}", "Server", Logger.Type.WARNING, e.stackTrace)
            }
            val bootstrap = ServerBootstrap()
            val bossGroup = NioEventLoopGroup()
            val workerGroup = NioEventLoopGroup()
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast("decoder", PacketDecoder())
                        ch.pipeline().addLast("encoder", PacketEncoder())
                        ch.pipeline().addLast("idleStateHandler", IdleStateHandler(60, 30, 0))
                        ch.pipeline().addLast("handler", ServerHandler())
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_SNDBUF, 4096 * 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
            CoroutineManager.register(RankingWorker(), 0, ServerConstants.rankingInterval.toLong())
            MonsterInformationProvider.clearDrops()

            logger.debug {"Loading skill data from XML."}
            SkillFactory.loadAllSkills()
            logger.info { "Successfully loading skill data from XML." }
            //timeToTake = System.currentTimeMillis()
            logger.debug { "Loading item data from XML." }
            val items = ItemInformationProvider.getAllItems()
            logger.info { "Successfully loading item data from XML. Total items: ${items.size}"}
            //timeToTake = System.currentTimeMillis()
            logger.debug { "Loading map custom life, cash item data from resources." }
            val customLife = MapFactory.loadCustomLife()
            CashShop.CashItemFactory.getItem(0)
            logger.info { "Successfully loading map custom life (total: $customLife), Cash shop item data." }
            //timeToTake = System.currentTimeMillis()
            logger.debug { "Loading server packet handlers." }
            ServerHandler.initiate()
            logger.info { "Successfully loading server packet handlers." }
            try {
                if (settings.worlds.size >= 21) {
                    logger.error { "World more than 21 cannot be start successful. Reduce some worlds. shutting down server." }
                    shutdown()
                }

                settings.worlds.forEachIndexed { i, w ->
                    logger.info { "Loading world. World number: $i, name: ${ServerConstants.worldNames[i]}" }
                    val world = World(
                        i, w.flag, w.eventMessage,
                        w.rates.exp, w.rates.drop, w.rates.meso, w.rates.bossDrop
                    )
                    worldRecommendedList.add(Pair(i, w.recommended))
                    worlds.add(world)
                    channels.add(mutableMapOf())
                    for (j in 0 until w.channelCount) {
                        val channelId = j + 1
                        val channel = Channel(i, channelId)
                        world.addChannel(channel)
                        channels[i][channelId] = channel.ip
                    }
                    world.setServerMessage(w.serverMessage)
                    autoSave(i)
                    logger.info { "World load completed. World number: $i" }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error caused when starting world and each channels. Exiting..." }
                e.printStackTrace()
                exitProcess(1)
            }
            /*acceptor.sessionConfig.setIdleTime(IdleStatus.BOTH_IDLE, 30)
            acceptor.handler = ServerHandler()*/
            bootstrap.bind(settings.bindHost, settings.bindPort)
            logger.info { "Login server opened: ${settings.bindHost}:${settings.bindPort}" }
            //acceptor.bind(InetSocketAddress(8484))
            if (settings.enableGMServer) GMServer.startGMServer()
            logger.info { "Server completely opened. Elapsed time: ${getElapsedTime(timeToTake)} sec." }
            online = true
        }
    }

    private fun allSave(world: Int) {
        getChannelsFromWorld(world).forEach { c ->
            c.players.getAllCharacters().forEach { it.saveToDatabase() }
        }
    }

    private fun autoSave(world: Int) {
        CoroutineManager.register({
            allSave(world)
            broadcastGMMessage(world, InteractPacket.serverNotice(5, "서버에 의해 월드가 저장되었습니다."))
            logger.info { "World $world successfully saved." }
        }, ServerConstants.autoSaveInterval.toLong(), ServerConstants.autoSaveInterval.toLong())
    }

    fun gmChat(message: String, exclude: String) {
        GMServer.broadcastInGame(InteractPacket.serverNotice(6, message))
        GMServer.broadcastOutGame(GMPacketCreator.chat(message), exclude)
    }

    fun shutdown() {
        //acceptor.unbind()
        logger.info { "Server is going offline." }
        //exitProcess(0)
    }

    fun shutdown(restart: Boolean): Runnable {
        return Runnable {
            logger.info { "${if (restart) "Restarting" else "Shutting down"} the server." }
            if (worlds.isEmpty()) return@Runnable
            worlds.forEach { it.shutdown() }
            worlds.forEach {
                while (it.players.getAllCharacters().isNotEmpty()) {
                    Thread.sleep(1000)
                }
            }
            getAllChannels().forEach {
                while (it.getConnectedClients() > 0) Thread.sleep(1000)
            }
            getAllChannels().forEach {
                while (!it.finishedShutdown) Thread.sleep(1000)
            }
            worlds.clear()
            channels.clear()
            worldRecommendedList.clear()
            //acceptor.unbind()
            logger.info { "Worlds and Channels are offline now." }
            if (!restart) {
                shutdown()
               // exitProcess(0)
            } else {
                logger.info { "Now starting restart process." }
                System.gc()
                run()
            }
        }
    }
}