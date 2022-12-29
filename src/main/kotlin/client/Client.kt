package client

import database.*
import gm.server.GMServer
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.util.AttributeKey
import kotlinx.coroutines.Job
import mu.KLogging
import net.RecvPacketOpcode
import net.server.Server
import net.server.guild.GuildCharacter
import net.server.world.MessengerCharacter
import net.server.world.PartyCharacter
import net.server.world.PartyOperation
import org.bouncycastle.util.encoders.Hex
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import scripting.npc.NPCScriptManager
import scripting.quest.QuestScriptManager
import server.Trade
import tools.CoroutineManager
import tools.PacketEncryption
import tools.PasswordHash
import tools.packet.CharacterPacket
import tools.packet.LoginPacket
import tools.packet.MiniGamePacket
import java.security.NoSuchAlgorithmException
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import javax.script.ScriptEngine

class Client(val sendCrypto: PacketEncryption, val receiveCrypto: PacketEncryption, val session: Channel) {
    var accountId = 1
    var accountName = ""
    var channel = 1
    private var socialNumber = 1234567
    var gender: Byte = -1
        set(value) {
            field = value
            try {
                transaction {
                    Accounts.update({ Accounts.id eq accountId }) { it[gender] = value.toInt() }
                }
            } catch (e: SQLException) {
                logger.warn(e) { "Failed to update account gender in database. AccountId: $accountId" }
            }
        }
    var characterSlots: Byte = 3
    private var lastPong = 0L
    private var loginAttempt = 0
    var gmLevel = 0
    var world = 0
    private var disconnecting = false
    var isConnector = false
    var loggedIn = false
    private var serverTransition = false
    private var birthday: Calendar? = null
    var player: Character? = null
    var idleTask: Job? = null
    val lock = ReentrantLock(true)
    private val engines = mutableMapOf<String, ScriptEngine>()
    private val macs = mutableSetOf<String>()
    var times = Array<Long>(255) { 0 }
    private var timesCounter = Array<Byte>(255) { 0 }

    @Synchronized
    fun announce(packet: ByteArray) {
        /*(val header = sendCrypto.getPacketHeader(packet.size)
        val encrypted = sendCrypto.encrypt(packet)
        //session?.write(header)
        session
         */
        session.writeAndFlush(packet)
        //session?.write(header + encrypted)*?
    }

    fun banMacs() {
        val filtered = mutableListOf<String>()
        try {
            /*loadMacsIfNecessary()
            var ps = con.prepareStatement("SELECT filter FROM macfilters")
            val rs = ps.executeQuery()
            while (rs.next()) {
                filtered.add(rs.getString("filter"))
            }
            ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)")
            macs.forEach {
                if (!filtered.contains(it)) {
                    ps.setString(1, it)
                    ps.executeUpdate()
                }
            }*/
        } catch (e: SQLException) {
            logger.error(e) { "Failed to handle banned MACs." }
        }
    }

    fun checkBirthDate(date: Calendar): Boolean {
        return birthday?.let {
            date[Calendar.YEAR] == it[Calendar.YEAR] &&
                    date[Calendar.MONTH] == it[Calendar.MONTH] &&
                    date[Calendar.DAY_OF_MONTH] == it[Calendar.DAY_OF_MONTH]
        } ?: false
    }

    fun dcConnect() = disconnect(shutdown = false, cashShop = false)

    fun deleteCharacter(cid: Int): Boolean {
        var result = false
        try {
            transaction {
                val row = Characters.select { (Characters.id eq cid) and (Characters.accountId eq accountId) }
                if (row.empty()) return@transaction
                val chr = row.first()
                if (chr[Characters.guildId] > 0) {
                    Server.deleteGuildCharacter(
                        GuildCharacter(
                            cid,
                            0,
                            chr[Characters.name],
                            -1,
                            -1,
                            0,
                            chr[Characters.guildRank],
                            chr[Characters.guildId],
                            false
                        )
                    )
                }
                KeyMap.deleteWhere { characterId eq cid }
                KeyValues.deleteWhere { KeyValues.cid eq cid }
                QuestStatuses.deleteWhere { characterId eq cid }
                Wishlists.deleteWhere { charId eq cid }
                FameLog.deleteWhere { characterId eq cid }
                InventoryItems.deleteWhere { characterId eq cid }
                QuestStatuses.deleteWhere { characterId eq cid }
                SavedLocations.deleteWhere { characterId eq cid }
                Skills.deleteWhere { characterId eq cid }
                EventStats.deleteWhere { characterId eq cid }
                Characters.deleteWhere { id eq cid }
                //SkillMacros.deleteWhere { SkillMacros.characterId eq cid }
                result = true
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to delete character in account. AccountId: $accountId, CharacterId: $cid" }
        }
        return result
    }

    fun disconnect(shutdown: Boolean, cashShop: Boolean) {
        if (disconnecting) return
        disconnecting = true
        if (player != null && player?.loggedIn == true/* && player?.client != null*/) {
            player?.let {
                val party = it.party
                val id = it.id
                val messengerId = it.messenger?.id ?: -1
                val cpc = PartyCharacter(it)
                val cmc = MessengerCharacter(it, 0)

                if (it.loggedIn) {
                    removePlayer()
                    it.saveToDatabase()
                    if (channel == -1 || shutdown) {
                        player = null
                        return
                    }
                    val world = getWorldServer()
                    try {
                        if (!cashShop) {
                            if (!serverTransition && messengerId != -1) {
                                world.leaveMessenger(messengerId, cmc)
                            }
                            it.getStartedQuests().forEach { status ->
                                val quest = status.quest
                                if (quest.timeLimit > 0) {
                                    val newStatus = QuestStatus(quest, QuestStatus.Status.NOT_STARTED)
                                    newStatus.forfeited = it.getQuest(quest).forfeited + 1
                                    it.updateQuest(newStatus)
                                }
                            }
                            if (party != null && !serverTransition) {
                                cpc.online = false
                                world.updateParty(party.id, PartyOperation.LOG_ONOFF, cpc)
                                if (party.leader.id == it.id) {
                                    var lchr: PartyCharacter? = null
                                    party.members.forEach { m ->
                                        if (m.id != null && it.map.getCharacterById(m.id) != null && (lchr == null || (lchr?.level
                                                ?: 255) < (m.level ?: 255))
                                        ) {
                                            lchr = m
                                        }
                                    }
                                    lchr?.let { it1 -> world.updateParty(party.id, PartyOperation.CHANGE_LEADER, it1) }
                                }
                            }
                            if (!this.serverTransition) {
                                world.buddyLoggedOff(it.id, channel, it.buddyList)
                            }
                            if (it.guildId > 0 && !serverTransition) {
                                it.mgc?.let { it1 -> Server.setGuildMemberOnline(it1, false, -1) }
                            }
                        } else {
                            party?.let { p ->
                                cpc.online = false
                                world.updateParty(p.id, PartyOperation.LOG_ONOFF, cpc)
                            }
                            if (!serverTransition) {
                                world.buddyLoggedOff(it.id, channel, it.buddyList)
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to handle disconnect client." }
                    } finally {
                        if (it.cashShop?.opened == true) getChannelServer().removePlayer(it)
                        if (!serverTransition) {
                            world.removePlayer(it)
                            it.disconnected()
                            it.empty(false)
                            it.logOff()
                        }
                        player = null
                    }
                }
            }
        }
        if (!serverTransition && loggedIn) {
            updateLoginState(LOGIN_NOTLOGGEDIN)
            session.attr(CLIENT_KEY)?.set(null)
            session.close()
        }
        engines.clear()
    }

    fun finishLogin(): Int {
        synchronized(Client) {
            if (getLoginState() > LOGIN_NOTLOGGEDIN) {
                loggedIn = false
                return 7
            }
            updateLoginState(LOGIN_LOGGEDIN)
        }
        return 0
    }

    fun gainCharacterSlot(): Boolean {
        var rst = false
        if (characterSlots < 15) {
            try {
                transaction {
                    Accounts.update({ Accounts.id eq accountId }) {
                        it[charactorSlots] = characterSlots + 1
                    }
                }
                characterSlots = (characterSlots + 1).toByte()
                rst = true
            } catch (e: SQLException) {
                logger.error(e) { "Failed to gain character slot. AccountId: $accountId" }
            }
        }
        return rst
    }

    fun getChannelServer() = Server.getChannel(world, channel)

    fun getChannelServer(channel: Byte) = Server.getChannel(world, channel.toInt())

    fun getCM() = NPCScriptManager.getCM(this)

    fun getGReason(): Byte {
        var value: Byte = 0
        try {
            transaction {
                val row = Accounts.slice(Accounts.gReason).select { Accounts.id eq accountId }
                if (row.empty()) return@transaction
                value = row.first()[Accounts.gReason].toByte()
            }
        } catch (e: SQLException) {
            logger.error { "Failed to get GReason. AccountId: $accountId" }
        }
        return value
    }

    fun getLoginState(): Int {
        var state = LOGIN_NOTLOGGEDIN
        try {
            transaction {
                val rs = Accounts.slice(Accounts.loggedIn, Accounts.lastLogin, Accounts.birthday).select {
                    Accounts.id eq accountId
                }
                if (rs.empty()) return@transaction
                val account = rs.first()
                val birthdayTime = account[Accounts.birthday]
                birthday = Calendar.getInstance()
                birthday?.timeInMillis = Timestamp.valueOf(birthdayTime.atStartOfDay()).time
                state = account[Accounts.loggedIn]
                if (state == LOGIN_SERVER_TRANSITION) {
                    if ((account[Accounts.lastLogin]?.toEpochMilli() ?: 0) + 30000 < System.currentTimeMillis()) {
                        state = LOGIN_NOTLOGGEDIN
                        updateLoginState(LOGIN_NOTLOGGEDIN)
                    }
                }
                when (state) {
                    LOGIN_LOGGEDIN -> {
                        loggedIn = true
                    }
                    LOGIN_SERVER_TRANSITION -> {
                        Accounts.update({ Accounts.id eq accountId }) { it[loggedIn] = 1 }
                    }
                    else -> loggedIn = false
                }
            }
        } catch (e: SQLException) {
            loggedIn = false
            logger.error(e) { "Error caused when getLoginState." }
        }
        return state
    }

    fun getQM() = QuestScriptManager.getQM(this)

    fun getSessionIPAddress() = session.remoteAddress().toString().split(":")[0]

    fun getScriptEngine(name: String) = engines[name]

    fun getTempBanCalendar(): Calendar? {
        var tempBan: Calendar? = null
        var timeInMilli = 0L
        try {
            transaction {
                val row = Accounts.slice(Accounts.tempBan).select { Accounts.id eq accountId }
                if (row.empty()) return@transaction
                val tb = row.first()[Accounts.tempBan]
                if (tb.toEpochMilli() == 0L) return@transaction
                tempBan = Calendar.getInstance()
                timeInMilli = tb.toEpochMilli()
            }
            if (timeInMilli > 0) {
                tempBan = Calendar.getInstance()
                tempBan?.timeInMillis = timeInMilli
            }
            return tempBan
        } catch (e: SQLException) {
            logger.error(e) { "Failed to get Temp ban calendar." }
        }
        return null
    }

    fun getWorldServer() = Server.getWorld(world)

    fun hasBannedIp(): Boolean {
        var ret = false
        try {
            val sessionIp = session.remoteAddress().toString()
            transaction {
                val ipBans = IPBans.slice(IPBans.id).select {
                    IPBans.ip like sessionIp
                }.count()

                if (ipBans > 0) ret = true
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to check banned IP." }
        }
        return ret
    }

    fun hasBannedMac(): Boolean {
        if (macs.isEmpty()) return false
        var ret = false
        try {
            transaction {
                val count = MacBans.select {
                    MacBans.mac inList macs
                }.count()
                if (count > 0) ret = true
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to check banned MAC." }
        }
        return ret
    }

    private fun loadCharactersInternal(serverId: Int): List<CharacterNameAndId> {
        try {
            val list = mutableListOf<CharacterNameAndId>()
            transaction {
                Characters.slice(Characters.id, Characters.name).select {
                    (Characters.accountId eq accountId) and (Characters.world eq serverId)
                }.forEach { list.add(CharacterNameAndId(it[Characters.id], it[Characters.name])) }
            }
            return list
        } catch (e: SQLException) {
            logger.error(e) { "Failed to get load Characters Internal. ServerId: $serverId" }
        }
        return emptyList()
    }

    fun loadCharacterNames(serverId: Int): List<String> {
        val list = mutableListOf<String>()
        loadCharactersInternal(serverId).forEach {
            list.add(it.name)
        }
        return list
    }

    fun loadCharacters(serverId: Int): List<Character> {
        val list = mutableListOf<Character>()
        loadCharactersInternal(serverId).forEach {
            val chr = Character.loadCharFromDatabase(it.id, this, false)
            if (chr != null) list.add(chr)
        }
        return list
    }

    fun loadMacsIfNecessary() {
        if (macs.isEmpty()) {
            transaction {
                val list = MacBans.select { MacBans.id eq accountId }.toList()
                if (list.isEmpty()) return@transaction
                list.first()[MacBans.mac].split(", ").forEach {
                    if (it != "") macs.add(it)
                }
            }
        }
    }

    fun login(login: String, password: String): Int {
        loginAttempt++
        if (loginAttempt > 4) session.close()
        var loginOk = 5
        try {
            transaction {
                val accounts = Accounts.slice(
                    Accounts.id, Accounts.password, Accounts.salt, Accounts.gender,
                    Accounts.banned, Accounts.gm, Accounts.charactorSlots, Accounts.tos,
                    Accounts.socialNumber
                ).select { Accounts.name eq login }
                if (!accounts.empty()) {
                    val acc = accounts.first()
                    if (acc[Accounts.banned]) {
                        loginOk = 3
                        return@transaction
                    }
                    accountId = acc[Accounts.id]
                    gmLevel = acc[Accounts.gm]
                    socialNumber = acc[Accounts.socialNumber]
                    gender = acc[Accounts.gender].toByte()
                    characterSlots = acc[Accounts.charactorSlots].toByte()
                    val passwordHash = acc[Accounts.password]
                    val salt = acc[Accounts.salt] ?: ""
                    val tos = acc[Accounts.tos]
                    if (checkHash(passwordHash, password, salt)) {
                        if (getLoginState() > LOGIN_NOTLOGGEDIN) {
                            loggedIn = false
                            loginOk = 7
                        } else loginOk = 0
                    } else {
                        loggedIn = false
                        loginOk = 4
                    }
                    IPLogs.insert {
                        it[accountId] = this@Client.accountId
                        it[ip] = session.remoteAddress().toString()
                    }
                } else return@transaction
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to handle login. LoginId: $login" }
        }
        if (loginOk == 0) loginAttempt = 0
        return loginOk
    }

    fun pongReceived() {
        lastPong = System.currentTimeMillis()
    }

    fun removePlayer() {
        try {
            player?.let { player ->
                if (!serverTransition) {
                    player.cancelAllBuffs(true)
                    player.cancelAllDeBuffs()
                    player.messenger = null
                    player.attackTick = 0
                    player.timeSet = 0
                }
                val mps = player.playerShop
                mps?.removeVisitors()
                val merchant = player.hiredMerchant
                if (merchant != null) {
                    if (merchant.isOwner(player))
                        merchant.open = true
                    else merchant.removeVisitor(player)
                    merchant.saveItems(false)
                }
                val game = player.miniGame
                if (game != null) {
                    player.miniGame = null
                    if (game.isOwner(player)) {
                        player.map.broadcastMessage(CharacterPacket.removeCharBox(player))
                        game.visitor?.client?.announce(MiniGamePacket.getMiniGameClose(true))
                    } else {
                        game.removeVisitor(player)
                    }
                }
                NPCScriptManager.dispose(this)
                QuestScriptManager.dispose(this)
                player.trade?.let { Trade.cancelTrade(player) }
                if (gmLevel > 0) GMServer.removeInGame(player.name)
                player.eventInstance?.playerDisconnected(player)
                player.map.removePlayer(player)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to remove player. AccountId: $accountId" }
        }
    }

    fun removeScriptEngine(name: String) = engines.remove(name)

    fun sendCharList(server: Int): ChannelFuture = session.writeAndFlush(CharacterPacket.getCharList(this, server))

    fun sendPing() {
        val then = System.currentTimeMillis()
        announce(LoginPacket.getPing())
        CoroutineManager.schedule({
            if (lastPong < then) {
                if (session.isOpen) {
                    session.close()
                }
            }
        }, 15000)
    }

    fun setScriptEngine(name: String, e: ScriptEngine?) = e?.let { engines.put(name, it) }

    fun setTimesByPacketId(packetId: Int) {
        when (packetId) {
            RecvPacketOpcode.PONG.value + 2,
            RecvPacketOpcode.CHANGE_MAP_SPECIAL.value - 1,
            RecvPacketOpcode.MESSENGER.value,
            RecvPacketOpcode.ITEM_PICKUP.value - 4,
            RecvPacketOpcode.MOVE_PLAYER.value,
            RecvPacketOpcode.MOVE_LIFE.value,
            RecvPacketOpcode.AUTO_AGGRO.value,
            RecvPacketOpcode.ITEM_PICKUP.value,
            RecvPacketOpcode.PET_LOOT.value,
            RecvPacketOpcode.MOVE_PET.value -> return
            else -> { }
        }
        val currentTime = System.currentTimeMillis()
        if (times[packetId] == 0L) {
            times[packetId] = currentTime
            return
        }
        val subTime = currentTime - times[packetId]
        if (subTime < 40) {
            timesCounter[packetId]++
            if (timesCounter[packetId] > 5) {
                times = Array(255) { 0 }
                timesCounter = Array(255) { 0 }
                disconnect(shutdown = false, cashShop = false)
            }
        }
        times[packetId] = currentTime
    }

    fun updateLoginState(newState: Int) {
        try {
            transaction {
                Accounts.update({ Accounts.id eq accountId }) {
                    it[loggedIn] = newState
                    it[lastLogin] = Instant.now()
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to update login state." }
        }
        if (newState == LOGIN_NOTLOGGEDIN) {
            loggedIn = false
            serverTransition = false
        } else {
            serverTransition = (newState == LOGIN_SERVER_TRANSITION)
            loggedIn = !serverTransition
        }
    }

    companion object : KLogging() {
        const val LOGIN_NOTLOGGEDIN = 0
        const val LOGIN_SERVER_TRANSITION = 1
        const val LOGIN_LOGGEDIN = 2
        val CLIENT_KEY = AttributeKey.valueOf<Client>("clientkey")

        fun checkHash(hash: String, password: String, salt: String): Boolean {
            try {
                val hashed = PasswordHash.generate(password, Hex.decode(salt))
                return hash == hashed
            } catch (e: NoSuchAlgorithmException) {
                logger.error(e) { "Failed to generate password hash." }
            }
            return false
        }
    }
}