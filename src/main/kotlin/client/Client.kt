package client

import database.*
import gm.server.GMServer
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.util.AttributeKey
import kotlinx.coroutines.Job
import net.RecvPacketOpcode
import net.server.Server
import net.server.guild.GuildCharacter
import net.server.world.MessengerCharacter
import net.server.world.PartyCharacter
import net.server.world.PartyOperation
import org.bouncycastle.util.encoders.Hex
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
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

/**
 * Represents a client in the system, managing user-related operations and network communication.
 * This class handles the client's session, authentication, character management, and server interaction.
 *
 * @property sendCrypto Manages encryption for outgoing packets.
 * @property receiveCrypto Manages decryption for incoming packets.
 * @property session The network session associated with the client.
 * @property accountId Unique identifier for the account linked to this client.
 * @property accountName The name of the account linked to this client.
 * @property channel The current channel the client is connected to.
 * @property socialNumber A unique identifier for social features, such as friend systems.
 * @property gender The gender selected by the client.
 * @property characterSlots Number of character slots available to the client.
 * @property lastPong Timestamp of the last "pong" message received, used for connection integrity checks.
 * @property loginAttempt Number of attempted logins, used for tracking security measures.
 * @property gmLevel The game master level of the client, used for administrative privileges.
 * @property world The world (server group) the client is currently connected to.
 * @property disconnecting A Boolean flag indicating if the client is in the process of disconnecting.
 * @property isConnector Indicates whether the client is a connector type session.
 * @property loggedIn A Boolean flag indicating if the client is currently logged in.
 * @property serverTransition A flag for when the player is transitioning between servers.
 * @property birthday The client's registered birthday, used for age-based restrictions.
 * @property player The player object associated with this client, representing in-game data.
 * @property idleTask A task triggered when the client is idle.
 * @property lock A lock object used for synchronizing client-related activities.
 * @property engines A collection of scripts or engines associated with this client.
 * @property macs A list of MAC addresses associated with this client, used for security.
 * @property times A data structure tracking event times for certain client interactions.
 * @property timesCounter A counter used for tracking repeated interactions.
 */
class Client(val sendCrypto: PacketEncryption, val receiveCrypto: PacketEncryption, val session: Channel) {
    /**
     * Logger instance using Kotlin Logging, configured with the default logger name derived from the enclosing class.
     */
    private val logger = KotlinLogging.logger {  }

    /**
     * Represents the unique identifier for an account associated with the client.
     * This is used to distinguish different accounts in the system and is set to
     * an initial default value of 1.
     */
    var accountId = 1
    /**
     * Represents the name associated with an account.
     * This variable typically stores a unique and identifiable name
     * assigned to the account for reference or display purposes.
     */
    var accountName = ""
    /**
     * Represents the channel to which the client is currently connected.
     * This variable plays a pivotal role in identifying the specific channel server
     * that the player interacts with during their session.
     */
    var channel = 1
    /**
     * A private variable representing the social number associated with the client.
     * This number may be used as a unique identifier or for verification purposes.
     * The value is initialized to an arbitrary number and may be updated as needed.
     */
    private var socialNumber = 1234567
    /**
     * Represents the gender associated with an account.
     *
     * The value is stored as a Byte, typically representing specific gender codes as defined
     * in the system, with the default value of `-1` indicating an unspecified or unknown gender.
     * When the value of this variable is updated, the corresponding gender field in the database
     * is updated for the account with the given `accountId`.
     *
     * If the database update fails due to an SQL exception, a warning is logged, providing
     * context about the failure along with the relevant `accountId`.
     */
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
    /**
     * Represents the number of character slots available for a client.
     * This value determines how many characters a player can create or maintain in their account.
     */
    var characterSlots: Byte = 3
    /**
     * Tracks the timestamp of the last Pong response received from the client.
     *
     * This variable is utilized to monitor the connection status of the client
     * by comparing the time elapsed since the last received Pong response.
     * It helps in detecting inactive or unresponsive clients and maintaining
     * a stable connection state.
     *
     * The value is stored as a `Long`, representing the time in milliseconds.
     */
    private var lastPong = 0L
    /**
     * Tracks the number of login attempts made by the client.
     *
     * This variable is used to monitor and control the number of times a client
     * has attempted to log in to the system. It may serve purposes such as
     * implementing security measures to prevent brute-force attacks or monitoring
     * user behavior.
     */
    private var loginAttempt = 0
    /**
     * Represents the game master level or rank of a user within the system.
     * The variable determines the permissions or privileges a user has.
     * A higher gmLevel typically indicates greater access and capabilities.
     *
     * Default value is 0, which may represent a standard or non-privileged user.
     */
    var gmLevel = 0
    /**
     * Represents a mutable integer variable named `world`.
     *
     * This variable can be used to store or manipulate integer values.
     * The default initial value of `world` is 0.
     */
    var world = 0
    /**
     * Indicates whether the client is in the process of disconnecting.
     * This variable helps manage the state of a client's connection
     * to prevent redundant or conflicting disconnection operations.
     */
    private var disconnecting = false
    /**
     * Indicates whether the client is configured as a connector.
     * A connector typically represents a special type of client that performs
     * connection or routing functions within the system.
     */
    var isConnector = false
    /**
     * Represents the login status of a user.
     * The value is `true` if the user is logged in, or `false` if the user is not logged in.
     */
    var loggedIn = false
    /**
     * Indicates whether the server transition state is active for the client.
     *
     * This flag represents the ongoing status of a character's transition
     * between servers. The value is typically used to determine if further
     * actions related to server-specific processes should proceed or be blocked.
     */
    private var serverTransition = false
    /**
     * The birthday of the client, represented as an optional Calendar object.
     * This variable may be used for validation, age calculations, or other features
     * that depend on the client's date of birth.
     */
    private var birthday: Calendar? = null
    /**
     * Represents the currently active character associated with the client.
     * This variable holds a nullable reference to a `Character` object that is set when the player logs in or selects a character.
     * It is used to track and manage the state of the associated player's character during the session.
     */
    var player: Character? = null
    /**
     * Represents a task that is executed when the client is idle.
     * This variable can hold a reference to a cancellable coroutine `Job`, allowing for the management of scheduled
     * or repeated actions during idle periods.
     *
     * If set to `null`, it indicates that no idle task is currently assigned or active.
     */
    var idleTask: Job? = null
    /**
     * A reentrant lock used to manage concurrent access to shared resources in the Client class.
     * This lock ensures thread safety by allowing synchronization of critical sections.
     * It is instantiated as a fair lock, meaning threads acquire the lock in the order they requested it.
     */
    val lock = ReentrantLock(true)
    /**
     * A map that stores registered script engines, identified by their unique names.
     * The key is a string representing the name of the script engine, and the value is the associated
     * `ScriptEngine` instance. This is used to manage and retrieve script engines within the client.
     */
    private val engines = mutableMapOf<String, ScriptEngine>()
    /**
     * A mutable set of strings used to store MAC addresses associated with the client.
     * Typically used to track and manage client-specific information such as linked devices for monitoring or restriction purposes.
     */
    private val macs = mutableSetOf<String>()
    /**
     * Array of timestamps used to track events or actions associated with the client.
     * Each element in the array represents a specific tracking point or event time,
     * initialized with a default value of 0.
     */
    var times = Array<Long>(255) { 0 }
    /**
     * Represents a counter used to store 8-bit values, initialized with a size of 255.
     * Each index in the array can be used to track or count specific occurrences or data points,
     * with all values starting at 0 by default.
     */
    private var timesCounter = Array<Byte>(255) { 0 }

    /**
     * Sends a packet to the connected session in a thread-safe manner.
     *
     * @param packet The packet data to be sent as a byte array.
     */
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

    /**
     * Bans the MAC addresses that are not in the filter list by adding them to the database.
     *
     * The method attempts to ban all MAC addresses associated with the client by ensuring
     * they are present in a specific database table for banned MAC addresses. It skips MAC
     * addresses that are already filtered.
     *
     * In case of a SQL-related issue during execution, an error is logged indicating
     * the failure to handle the banned MAC addresses.
     *
     * Internally, operations to load necessary MAC addresses and interact with the database are commented out,
     * leaving the logging mechanism active for error handling.
     */
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

    /**
     * Validates if a given calendar date matches the client's stored birthdate.
     *
     * @param date the calendar date to be checked against the client's birthdate.
     * @return true if the given date matches the client's birthdate; false otherwise.
     */
    fun checkBirthDate(date: Calendar): Boolean {
        return birthday?.let {
            date[Calendar.YEAR] == it[Calendar.YEAR] &&
                    date[Calendar.MONTH] == it[Calendar.MONTH] &&
                    date[Calendar.DAY_OF_MONTH] == it[Calendar.DAY_OF_MONTH]
        } ?: false
    }

    /**
     * Facilitates a disconnection process for the client while maintaining the current session state.
     *
     * This method invokes the `disconnect` method with predefined parameters to avoid shutting down
     * the session or disrupting the cash shop environment. It is typically used when a client needs to
     * transition or reconnect without triggering a complete logout or termination of the client instance.
     */
    fun dcConnect() = disconnect(shutdown = false, cashShop = false)

    /**
     * Deletes a character associated with the given character ID (cid).
     * It removes all related data and associations from various tables in the database.
     *
     * @param cid The unique identifier of the character to be deleted.
     * @return True if the character was successfully deleted, false otherwise.
     */
    fun deleteCharacter(cid: Int): Boolean {
        return Companion.deleteCharacter(cid, accountId)
    }

    /**
     * Disconnects the player from the server and handles all related cleanup operations.
     *
     * @param shutdown Indicates whether the disconnection is part of a server shutdown process.
     * @param cashShop Specifies if the player is being disconnected while in the cash shop.
     */
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

    /**
     * Completes the login process for the client by updating the login state.
     * If the client is already logged in, the method sets the logged-in status to false
     * and returns a specific code indicating that the login cannot be completed.
     *
     * @return 0 if the login process is successfully completed, or 7 if the client is
     *         already logged in and the login cannot proceed.
     */
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

    /**
     * Attempts to gain an additional character slot for the account, provided the maximum limit has not been reached.
     *
     * This method checks whether the current number of character slots is less than 15.
     * If so, it attempts to increment the character slot count in the database and update the corresponding in-memory value.
     * If an error occurs during the database update, it logs the error and no changes are made.
     *
     * @return `true` if a character slot was successfully gained, otherwise `false`.
     */
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

    /**
     * Retrieves the server instance associated with the specified world and channel.
     *
     * @return The server instance corresponding to the given world and channel.
     */
    fun getChannelServer() = Server.getChannel(world, channel)

    /**
     * Retrieves the server instance for the given channel.
     *
     * @param channel The channel identifier as a byte value for which the server instance is to be retrieved.
     * @return The server instance corresponding to the given channel.
     */
    fun getChannelServer(channel: Byte) = Server.getChannel(world, channel.toInt())

    /**
     * Retrieves the conversation manager (CM) associated with the client.
     *
     * The conversation manager is typically used to manage NPC
     * interactions and scripting contexts for the client.
     *
     * @return The conversation manager instance for the client.
     */
    fun getCM() = NPCScriptManager.getCM(this)

    /**
     * Retrieves the gReason value associated with the current account.
     * The value is fetched from the database using the account ID.
     * In case of an SQL exception, it logs an error message.
     *
     * @return The gReason value as a Byte. Returns 0 if no value is found or an exception occurs.
     */
    fun getGReason(): Byte {
        var value: Byte = 0
        try {
            transaction {
                val row = Accounts.select(Accounts.gReason).where { Accounts.id eq accountId }
                if (row.empty()) return@transaction
                value = row.first()[Accounts.gReason].toByte()
            }
        } catch (e: SQLException) {
            logger.error { "Failed to get GReason. AccountId: $accountId" }
        }
        return value
    }

    /**
     * Retrieves the current login state of the account.
     * The login state is determined based on the account's database record and current session status.
     * It also handles transitions or updates the state in certain scenarios, such as when the server
     * transition timeout expires.
     *
     * @return An integer representing the login state. Possible states include:
     * - `LOGIN_NOTLOGGEDIN`: The account is not logged in.
     * - `LOGIN_LOGGEDIN`: The account is logged in.
     * - `LOGIN_SERVER_TRANSITION`: The account is in a server transition state.
     */
    fun getLoginState(): Int {
        var state = LOGIN_NOTLOGGEDIN
        try {
            transaction {
                val rs = Accounts.select(Accounts.loggedIn, Accounts.lastLogin, Accounts.birthday).where {
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

    /**
     * Retrieves the QuestScriptManager instance associated with the current client.
     * This instance is used for managing quests and related scripts.
     *
     * @return The QuestScriptManager instance for the client.
     */
    fun getQM() = QuestScriptManager.getQM(this)

    /**
     * Retrieves the IP address of the remote client connected to this session.
     *
     * Extracts the IP address from the session's remote address string, stripping
     * the port number that follows the colon separator. For example, given a remote
     * address of `/192.168.1.1:54321`, this method returns `"/192.168.1.1"`.
     *
     * @return The IP address portion of the remote address as a [String],
     *         without the port number.
     */
    fun getSessionIPAddress() = session.remoteAddress().toString().split(":")[0]

    /**
     * Retrieves a script engine by its name.
     *
     * @param name the name of the script engine to retrieve
     */
    fun getScriptEngine(name: String) = engines[name]

    /**
     * Retrieves the temporary ban date for the associated account as a Calendar instance.
     * If no temporary ban exists or an error occurs during retrieval, null is returned.
     *
     * @return A Calendar instance representing the temporary ban date, or null if there is no ban or an error occurs.
     */
    fun getTempBanCalendar(): Calendar? {
        var tempBan: Calendar? = null
        var timeInMilli = 0L
        try {
            transaction {
                val row = Accounts.select(Accounts.tempBan).where { Accounts.id eq accountId }
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

    /**
     * Retrieves the world server associated with the client's current world.
     *
     * This method interacts with the central server to obtain the instance of
     * the `World` corresponding to the client's `world` property. It provides
     * access to the specific server instance that manages all activities related
     * to the given world.
     *
     * @return the `World` server instance corresponding to the client's world.
     */
    fun getWorldServer() = Server.getWorld(world)

    /**
     * Checks whether the IP address of the current session is banned.
     *
     * Retrieves the full remote address string from the session and queries
     * the [database.IPBans] table using a `LIKE` match. If one or more
     * matching entries are found, the IP is considered banned.
     *
     * Any [java.sql.SQLException] encountered during the database query is
     * logged as an error and the method returns `false`.
     *
     * @return `true` if the session's IP address is found in the banned IP list,
     *         `false` otherwise or if a database error occurs.
     */
    fun hasBannedIp(): Boolean {
        var ret = false
        try {
            val sessionIp = session.remoteAddress().toString()
            transaction {
                val ipBans = IPBans.select(IPBans.id).where {
                    IPBans.ip like sessionIp
                }.count()

                if (ipBans > 0) ret = true
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to check banned IP." }
        }
        return ret
    }

    /**
     * Checks if any of the stored MAC addresses are banned.
     *
     * The method verifies if the client's list of MAC addresses contains any that match entries
     * in the `MacBans` database table. If a match is found, it indicates that one or more of
     * the MAC addresses are banned.
     *
     * @return true if at least one MAC address is banned, false otherwise
     */
    fun hasBannedMac(): Boolean {
        if (macs.isEmpty()) return false
        var ret = false
        try {
            transaction {
                val count = MacBans.selectAll().where {
                    MacBans.mac inList macs
                }.count()
                if (count > 0) ret = true
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to check banned MAC." }
        }
        return ret
    }

    /**
     * Loads and retrieves a list of characters for a given server ID.
     *
     * @param serverId The ID of the server to load characters from.
     * @return A list of characters, represented as instances of CharacterNameAndId,
     *         or an empty list if an error occurs.
     */
    private fun loadCharactersInternal(serverId: Int): List<CharacterNameAndId> {
        try {
            val list = mutableListOf<CharacterNameAndId>()
            transaction {
                Characters.select(Characters.id, Characters.name).where {
                    (Characters.accountId eq accountId) and (Characters.world eq serverId)
                }.forEach { list.add(CharacterNameAndId(it[Characters.id], it[Characters.name])) }
            }
            return list
        } catch (e: SQLException) {
            logger.error(e) { "Failed to get load Characters Internal. ServerId: $serverId" }
        }
        return emptyList()
    }

    /**
     * Loads a list of character names associated with the specified server ID.
     *
     * @param serverId The ID of the server for which to load character names.
     * @return A list of character names found for the given server ID. Returns an empty list if no characters are found or an error occurs.
     */
    fun loadCharacterNames(serverId: Int): List<String> {
        val list = mutableListOf<String>()
        loadCharactersInternal(serverId).forEach {
            list.add(it.name)
        }
        return list
    }

    /**
     * Loads a list of characters associated with a specific server ID.
     *
     * @param serverId The identifier of the server from which the characters are to be loaded.
     * @return A list of Character objects belonging to the specified server. If no characters are found, an empty list is returned.
     */
    fun loadCharacters(serverId: Int): List<Character> {
        val list = mutableListOf<Character>()
        loadCharactersInternal(serverId).forEach {
            val chr = Character.loadCharFromDatabase(it.id, this, false)
            if (chr != null) list.add(chr)
        }
        return list
    }

    /**
     * Loads MAC addresses associated with the current client account if they have not already been loaded.
     *
     * This function checks the internal `macs` collection to determine if it is empty. If it is, a database
     * transaction is initiated to load the MAC addresses linked to the account ID from the `MacBans` table.
     * The retrieved data is split and each non-empty MAC address is added to the `macs` collection.
     *
     * Does nothing if the MAC addresses are already available.
     */
    fun loadMacsIfNecessary() {
        if (macs.isEmpty()) {
            transaction {
                val list = MacBans.selectAll().where { MacBans.id eq accountId }.toList()
                if (list.isEmpty()) return@transaction
                list.first()[MacBans.mac].split(", ").forEach {
                    if (it != "") macs.add(it)
                }
            }
        }
    }

    /**
     * Handles the login process for a user using their credentials.
     *
     * @param login The username of the account attempting to log in.
     * @param password The password associated with the username.
     * @return An integer code representing the login result:
     *         0 - Login successful.
     *         3 - Account banned.
     *         4 - Incorrect password.
     *         5 - Login failed due to database or other issues.
     *         7 - User is already logged in.
     */
    fun login(login: String, password: String): Int {
        loginAttempt++
        if (loginAttempt > 4) session.close()
        var loginOk = 5
        try {
            transaction {
                val accounts = Accounts.select(
                    Accounts.id, Accounts.password, Accounts.salt, Accounts.gender,
                    Accounts.banned, Accounts.gm, Accounts.charactorSlots, Accounts.tos,
                    Accounts.socialNumber
                ).where { Accounts.name eq login }
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

    /**
     * Updates the timestamp of the last received pong message to the current system time.
     *
     * This function is typically called when the client receives a pong message
     * in response to a ping request, ensuring that the connection is active and
     * responsive. The `lastPong` field is updated to represent the time the response
     * was received, measured in milliseconds since the Unix epoch.
     */
    fun pongReceived() {
        lastPong = System.currentTimeMillis()
    }

    /**
     * Removes the current player from the game session and performs various cleanup operations.
     *
     * This method ensures that the player is properly removed from the game environment by:
     * - Canceling all buffs and debuffs for the player if they are not in server transition.
     * - Releasing the player's associated messenger.
     * - Resetting certain player-related properties such as attack tick and time set.
     * - Properly handling any player shops or hired merchants that the player is involved in.
     * - Managing the player's interaction with mini-games, closing the session or removing the player as a visitor when necessary.
     * - Disposing of any NPC or quest scripts currently active for the player.
     * - Canceling active trades the player is part of.
     * - Removing the player from any GM-related tracking if applicable.
     * - Disconnecting the player from any event instances they are part of.
     * - Finally, removing the player from the map.
     *
     * If an error occurs during the process, it will be logged for debugging purposes.
     */
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

    /**
     * Removes a script engine with the specified name from the script engine manager.
     *
     * @param name The name of the script engine to be removed.
     */
    fun removeScriptEngine(name: String) = engines.remove(name)

    /**
     * Sends the list of characters available for the specified server to the client.
     *
     * @param server The ID of the server for which to retrieve and send the character list.
     * @return A `ChannelFuture` representing the asynchronous operation of sending the character list packet.
     */
    fun sendCharList(server: Int): ChannelFuture = session.writeAndFlush(CharacterPacket.getCharList(this, server))

    /**
     * Sends a ping packet to the server and schedules a task to monitor for a pong response.
     *
     * This method sends a ping packet using the `announce` function, triggering server communication to check connectivity.
     * It then schedules a coroutine to check if a pong response has been received within 15 seconds.
     * If no pong response is detected (`lastPong` < timestamp of the sent ping) and the session is still open,
     * the session is forcibly closed to prevent further operations on the disconnected client.
     */
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

    /**
     * Sets a script engine instance associated with a given name.
     * If the engine is not null, it is stored in the internal engine map.
     *
     * @param name The unique name to associate with the script engine.
     * @param e The script engine instance to be stored, or null to skip storing.
     */
    fun setScriptEngine(name: String, e: ScriptEngine?) = e?.let { engines.put(name, it) }

    /**
     * Updates and manages timing information for a specific packet ID. Ensures that packet processing adheres
     * to timing constraints, potentially disconnecting in case of abnormal behavior.
     *
     * @param packetId The unique identifier of the packet whose timing information is being updated.
     */
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

    /**
     * Updates the login state of the client and adjusts related fields based on the new state.
     *
     * @param newState The new login state to be set. Valid values represent different states
     * such as logged in, not logged in, or transitioning between servers.
     */
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

    /**
     * Companion object providing constants, functions, and logging for managing login states, clients,
     * and character-related operations.
     */
    companion object {
        /**
         * Logger instance for recording application events and diagnostics.
         */
        private val logger = KotlinLogging.logger {  }

        /**
         * Represents the state of a client when it is not logged in.
         * This value indicates that the client has not successfully authenticated,
         * and no session has been established.
         */
        const val LOGIN_NOTLOGGEDIN = 0
        /**
         * Represents the constant value used to indicate a transition state
         * for the login server process or operation. This value is typically
         * utilized in scenarios where the state of the login server needs
         * to be tracked or identified as transitioning.
         */
        const val LOGIN_SERVER_TRANSITION = 1
        /**
         * Represents the state of a user being logged in.
         *
         * This constant is used to indicate that the user has successfully logged
         * into the system. It is typically utilized in scenarios where login state
         * needs to be tracked or verified.
         *
         * Value: 2
         */
        const val LOGIN_LOGGEDIN = 2
        /**
         * CLIENT_KEY is an attribute key used to store and retrieve a Client object
         * in a context that supports attribute storage.
         * It serves as a unique identifier for accessing the associated Client instance.
         */
        val CLIENT_KEY = AttributeKey.valueOf<Client>("clientkey")

        /**
         * Verifies if the provided hash matches the hash generated for the given password and salt.
         *
         * @param hash The hash to be compared.
         * @param password The plain text password.
         * @param salt The salt used for hashing.
         * @return `true` if the provided hash matches the generated hash, `false` otherwise.
         */
        fun checkHash(hash: String, password: String, salt: String): Boolean {
            try {
                val hashed = PasswordHash.generate(password, Hex.decode(salt))
                return hash == hashed
            } catch (e: NoSuchAlgorithmException) {
                logger.error(e) { "Failed to generate password hash." }
            }
            return false
        }

        /**
         * Deletes a character with the specified character ID and account ID.
         * This method performs multiple database operations to remove the character and
         * all associated data, such as quests, skills, inventory, and guild membership.
         *
         * @param cid the ID of the character to be deleted
         * @param accountId the ID of the account associated with the character
         * @return true if the character was deleted successfully, false otherwise
         */
        fun deleteCharacter(cid: Int, accountId: Int): Boolean {
            var result = false
            try {
                transaction {
                    val row = Characters.selectAll().where { (Characters.id eq cid) and (Characters.accountId eq accountId) }
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
    }
}