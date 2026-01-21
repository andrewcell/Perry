package net.server.guild

import client.Client
import database.Characters
import database.Guilds
import database.Notes
import mu.KLogging
import net.server.Server
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import tools.packet.GuildPacket
import tools.packet.InteractPacket
import java.sql.SQLException
import java.time.Instant

class Guild(creator: GuildCharacter) {
    val members = mutableListOf<GuildCharacter>()
    private val rankTitles = arrayOf("길드마스터", "부마스터", "길드원", "길드원", "길드원")
    val world = creator.world
    var id = creator.guildId
    var name: String = ""
    var notice: String = ""
    var gp: Int = -1
    var logo: Short = -1
    var logoColor: Byte = -1
    var leader: Int = -1
    var capacity: Int = -1
    var logoBG: Short = -1
    var logoBGColor: Byte = -1
    var signature: Int = -1
    private val notifications = mutableMapOf<Int, MutableList<Int>>()
    private var dirty = true

    init {
        try {
            transaction {
                val row = Guilds.selectAll().where { Guilds.guildId eq this@Guild.id }
                if (row.empty()) {
                    this@Guild.id = -1
                    return@transaction
                }
                val guild = row.first()
                name = guild[Guilds.name]
                gp = guild[Guilds.GP]
                logo = guild[Guilds.logo].toShort()
                logoColor = guild[Guilds.logoColor].toByte()
                logoBG = guild[Guilds.logoBG].toShort()
                logoBGColor = guild[Guilds.logoBGColor].toByte()
                capacity = guild[Guilds.capacity]
                rankTitles[0] = guild[Guilds.rank1Title]
                rankTitles[1] = guild[Guilds.rank2Title]
                rankTitles[2] = guild[Guilds.rank3Title]
                rankTitles[3] = guild[Guilds.rank4Title]
                rankTitles[4] = guild[Guilds.rank5Title]
                leader = guild[Guilds.leader]
                notice = guild[Guilds.notice]
                signature = guild[Guilds.signature]
                Characters.select(
                    Characters.id,
                    Characters.level,
                    Characters.name,
                    Characters.job,
                    Characters.guildRank
                ).where { Characters.guildId eq this@Guild.id }.orderBy(Characters.guildRank, SortOrder.ASC)
                    .orderBy(Characters.name, SortOrder.ASC).forEach {
                    members.add(
                        GuildCharacter(
                            it[Characters.id],
                            it[Characters.level],
                            it[Characters.name],
                            -1,
                            world,
                            it[Characters.job],
                            it[Characters.guildRank],
                            this@Guild.id,
                            false
                        )
                    )
                }
            }
            setOnline(creator.id, true, creator.channel)
        } catch (e: SQLException) {
            logger.error(e) { "Failed to init guild data from database." }
        }
    }

    private fun buildNotifications() {
        if (!dirty) return
        val chs = Server.getChannelServer(world)
        if (notifications.keys.size != chs.size) {
            notifications.clear()
            chs.forEach { notifications[it] = mutableListOf() }
        } else {
            notifications.values.forEach { it.clear() }
        }
        synchronized(members) {
            members.forEach {
                if (!it.online) return@forEach
                notifications[it.channel]?.add(it.id)
            }
        }
        dirty = false
    }

    private fun writeToDatabase(disband: Boolean) {
        try {
            transaction {
                if (!disband) {
                    Guilds.update({ Guilds.guildId eq this@Guild.id }) {
                        it[GP] = this@Guild.gp
                        it[logo] = this@Guild.logo.toInt()
                        it[logoBG] = this@Guild.logoBG.toInt()
                        it[logoBGColor] = this@Guild.logoBGColor.toShort()
                        it[logoColor] = this@Guild.logoColor.toShort()
                        it[rank1Title] = rankTitles[0]
                        it[rank2Title] = rankTitles[1]
                        it[rank3Title] = rankTitles[2]
                        it[rank4Title] = rankTitles[3]
                        it[rank5Title] = rankTitles[4]
                        it[capacity] = this@Guild.capacity
                        it[notice] = this@Guild.notice
                    }
                } else {
                    Characters.update({ Characters.guildId eq this@Guild.id }) {
                        it[guildId] = 0
                        it[guildRank] = 0
                    }
                    Guilds.deleteWhere { Guilds.guildId eq this@Guild.id }
                    broadcast(GuildPacket.guildDisband(this@Guild.id))
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save guild data to database." }
        }
    }

    fun broadcast(packet: ByteArray?, exceptionId: Int = -1, bCop: BCOp = BCOp.NONE) {
        synchronized(notifications) {
            if (dirty) buildNotifications()
            try {
                Server.getChannelServer(world).forEach {
                    val notification = notifications[it] ?: return@forEach
                    if (notification.size > 0) {
                        when (bCop) {
                            BCOp.DISBAND -> Server.getWorld(world).setGuildAndRank(notification, 0, 5, exceptionId)
                            BCOp.EMBLEMCHANGE -> Server.getWorld(world)
                                .changeGuildEmblem(id, notification, GuildSummary(this))

                            else -> packet?.let { it1 ->
                                Server.getWorld(world).sendPacket(
                                    notification,
                                    it1, exceptionId
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to broadcast packet to guild members." }
            }
        }
    }

    fun guildMessage(serverNotice: ByteArray) {
        members.forEach { m ->
            run ch@{
                Server.getChannelsFromWorld(world).forEach { c ->
                    c.players.getCharacterById(m.id)?.client?.announce(serverNotice)
                    return@ch
                }
            }
        }
    }

    fun guildChat(name: String, cid: Int, message: String) = broadcast(InteractPacket.multiChat(name, message, 2), cid)

    fun setOnline(cid: Int, online: Boolean, channel: Int) {
        var broadcast = true
        run loop@{
            members.forEach {
                if (it.id == cid) {
                    if (it.online && online) {
                        broadcast = false
                    }
                    it.online = online
                    it.channel = channel
                    return@loop
                }
            }
        }
        if (broadcast) broadcast(GuildPacket.guildMemberOnline(id, cid, online), cid)
        dirty = true
    }

    fun getRankTitle(rank: Int) = rankTitles[rank - 1]

    fun getIncreaseGuildCost(size: Int) = 500000 * (size - 6) / 6

    fun addGuildMember(mgc: GuildCharacter): Int {
        synchronized(members) {
            if (members.size >= capacity) return 0
            for (i in members.indices.reversed()) {
                if (members[i].guildRank < 5 || members[i].name < mgc.name) {
                    members.add(i + 1, mgc)
                    dirty = true
                    break
                }
            }
        }
        broadcast(GuildPacket.newGuildMember(mgc))
        return 1
    }

    fun leaveGuild(mgc: GuildCharacter) {
        broadcast(GuildPacket.guildMemberLeft(mgc, false))
        synchronized(members) {
            members.remove(mgc)
            dirty = true
        }
    }

    fun expelMember(init: GuildCharacter, name: String, cid: Int) {
        synchronized(members) {
            members.forEach {
                if (it.id == cid && init.guildRank < it.guildRank) {
                    broadcast(GuildPacket.guildMemberLeft(it, true))
                    members.remove(it)
                    dirty = true
                    try {
                        if (it.online) {
                            Server.getWorld(it.world).setGuildAndRank(cid, 0, 5)
                        } else {
                            try {
                                transaction {
                                    Notes.insert { it1 ->
                                        it1[to] = name
                                        it1[from] = init.name
                                        it1[message] = "길드에서 강퇴당하셨습니다."
                                        it1[timestamp] = Instant.now()
                                    }
                                }
                            } catch (e: SQLException) {
                                logger.error(e) { "Failed to send expelled message to character due to database error." }
                            }
                            Server.getWorld(it.world).setOfflineGuildStatus(0, 5, cid)
                        }
                    } catch (e: Exception) {
                        logger.error { "Error caused when expel guild member." }
                    }
                }
            }
        }
    }

    fun changeRank(cid: Int, newRank: Int) {
        val target = members.find { cid == it.id } ?: return
        if (target.online) {
            Server.getWorld(target.world).setGuildAndRank(cid, id, newRank)
        } else {
            Server.getWorld(target.world).setOfflineGuildStatus(id, newRank, cid)
        }
        target.guildRank = newRank
        broadcast(GuildPacket.changeGuildRank(target))
    }

    fun setGuildNotice(notice: String) {
        this.notice = notice
        writeToDatabase(false)
        broadcast(GuildPacket.guildNotice(id, notice))
    }

    fun memberLevelJobUpdate(mgc: GuildCharacter) {
        val target = members.find { it == mgc } ?: return
        target.jobId = mgc.jobId
        target.level = mgc.level
        broadcast(GuildPacket.guildMemberLevelJobUpdate(mgc))
    }

    fun changeRankTitle(ranks: Array<String>) {
        System.arraycopy(ranks, 0, rankTitles, 0, 5)
        broadcast(GuildPacket.rankTitleChange(id, ranks))
        writeToDatabase(false)
    }

    fun disbandGuild() {
        writeToDatabase(true)
        broadcast(null, -1, BCOp.DISBAND)
    }

    fun setGuildEmblem(bg: Short, bgColor: Byte, logo: Short, logoColor: Byte) {
        logoBG = bg
        logoBGColor = bgColor
        this.logo = logo
        this.logoColor = logoColor
        writeToDatabase(false)
        broadcast(null, -1, BCOp.EMBLEMCHANGE)
    }

    fun increaseCapacity(): Boolean {
        if (capacity > 99) return false
        capacity += 5
        writeToDatabase(false)
        broadcast(GuildPacket.guildCapacityChange(id, capacity))
        return true
    }

    enum class BCOp {
        NONE, DISBAND, EMBLEMCHANGE
    }

    companion object : KLogging() {
        const val CREATE_GUILD_COST = 1500000
        const val CHANGE_EMBLEM_COST = 5000000

        fun createGuild(leaderId: Int, name: String): Int {
            var result = 0
            try {
                transaction {
                    val row = Guilds.select(Guilds.guildId).where { Guilds.name eq name }
                    if (!row.empty()) return@transaction // check name exists.
                    Guilds.insert {
                        it[leader] = leaderId
                        it[Guilds.name] = name
                        it[signature] = System.currentTimeMillis().toInt()
                    }
                    val guild = Guilds.select(Guilds.guildId).where { Guilds.leader eq leaderId }
                    if (guild.empty()) throw Exception("Tried to insert to database for create guild, but returned nothing.")
                    result = guild.first()[Guilds.guildId]
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to create guild." }
            }
            return result
        }

        fun sendInvite(c: Client, targetName: String): GuildResponse? {
            val mc = c.getChannelServer().players.getCharacterByName(targetName) ?: return GuildResponse.NOT_IN_CHANNEL
            if (mc.guildId > 0) return GuildResponse.ALREADY_IN_GUILD
            c.player?.let { mc.client.announce(GuildPacket.guildInvite(it.guildId, it.name)) }
            return null
        }

        fun displayGuildRanks(c: Client, npcId: Int) {
            try {
                transaction {
                    val row = Guilds.selectAll().orderBy(Guilds.GP, SortOrder.DESC).limit(50)
                    c.announce(GuildPacket.showGuildRanks(npcId, row.toList()))
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to get guilds data from database." }
            }
        }
    }
}