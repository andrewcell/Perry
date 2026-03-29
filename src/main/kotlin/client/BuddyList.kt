package client

import database.Buddies
import database.Characters
import mu.KLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tools.packet.InteractPacket
import java.sql.SQLException

/**
 * Manages the buddy list for a character, including active buddies and pending requests.
 *
 * @param capacity The maximum number of buddies allowed in the list.
 */
class BuddyList(var capacity: Int) {
    /**
     * Map of buddy entries indexed by their character ID.
     */
    val buddies = mutableMapOf<Int, BuddyListEntry>()

    /**
     * List of pending buddy requests (not yet accepted or added).
     */
    val pendingRequests = mutableListOf<CharacterNameAndId>()

    /**
     * Checks whether the specified character ID exists in the buddy list.
     *
     * @param characterId The character ID to check.
     * @return `true` if the character is present, `false` otherwise.
     */
    fun contains(characterId: Int) = buddies.containsKey(characterId)

    /**
     * Checks whether the specified character's entry exists and is visible.
     *
     * @param characterId The character ID to check visibility for.
     * @return `true` if the entry exists and `visible` is `true`, `false` otherwise.
     */
    fun containsVisible(characterId: Int) = buddies[characterId]?.visible ?: false

    //    fun getEntry(cId: Int) = buddies[cId]

    /**
     * Retrieves a buddy entry by name (case-insensitive match).
     *
     * @param name The name to search for (case-insensitive).
     * @return The first matching [BuddyListEntry], or `null` if not found.
     */
    fun getEntryByName(name: String) = buddies.values.find { it.name == name.lowercase() }

    /**
     * Adds or replaces a buddy entry in the list.
     *
     * @param e The [BuddyListEntry] to add.
     * @return The previous entry associated with the character ID, or `null` if none existed.
     */
    fun addEntry(e: BuddyListEntry) = buddies.put(e.characterId, e)

    /**
     * Removes a buddy entry by character ID.
     *
     * @param characterId The ID of the buddy to remove.
     * @return The removed entry, or `null` if not found.
     */
    fun remove(characterId: Int) = buddies.remove(characterId)

    /**
     * Checks whether the buddy list has reached its capacity.
     *
     * @return `true` if the number of entries is at or above [capacity], `false` otherwise.
     */
    fun isFull() = buddies.size >= capacity

    /**
     * Returns a list of all buddy character IDs currently in the list.
     *
     * @return A list of character IDs ([Int]).
     */
    fun getBuddyIds() = buddies.keys.toList()

    /**
     * Loads buddy data and pending requests for this character from the database.
     *
     * Existing entries are loaded into [buddies] and [pendingRequests].
     * All pending entries (marked with `pending = 1`) are deleted after loading.
     *
     * @param characterId The ID of the character whose buddy list should be loaded.
     */
    fun loadFromDatabase(characterId: Int) {
        try {
            transaction {
                (Buddies innerJoin Characters)
                    .select(Buddies.buddyId, Buddies.pending, Buddies.group, Characters.name)
                    .where { (Characters.id eq characterId) and (Buddies.buddyId eq Characters.id) }
                    .forEach {
                        if (it[Buddies.pending].toInt() == 1) {
                            pendingRequests.add(CharacterNameAndId(it[Buddies.buddyId], it[Characters.name]))
                        } else {
                            addEntry(
                                BuddyListEntry(
                                    it[Characters.name],
                                    it[Buddies.group],
                                    it[Buddies.buddyId],
                                    1,
                                    true
                                )
                            )
                        }
                    }
                Buddies.deleteWhere { (pending eq 1) and (Buddies.characterId eq characterId) }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to load Buddy list from database." }
        }
    }

    /**
     * Removes and returns the oldest pending buddy request.
     *
     * @return The most recently added [CharacterNameAndId] from pending requests.
     */
    fun pollPendingRequest() = pendingRequests.removeLast()

    /**
     * Adds a new buddy request to this list.
     *
     * A default group name `"그룹 미지정"` is used. If no other requests are pending,
     * an announcement packet is sent immediately to the provided [Client];
     * otherwise, the request is queued in [pendingRequests].
     *
     * @param c The client representing the sender of the request.
     * @param cidFrom The character ID of the requesting user.
     * @param nameFrom The name of the requesting user.
     * @param channelFrom The channel from which the request originated.
     */
    fun addBuddyRequest(c: Client, cidFrom: Int, nameFrom: String, channelFrom: Int) {
        addEntry(BuddyListEntry(nameFrom, "그룹 미지정", cidFrom, channelFrom, false))
        if (pendingRequests.isEmpty()) {
            c.player?.let { c.announce(InteractPacket.requestBuddyListAdd(cidFrom, it.id, nameFrom)) }
        } else {
            pendingRequests.add(CharacterNameAndId(cidFrom, nameFrom))
        }
    }

    /**
     * Represents possible outcomes of a buddy operation (e.g., add/delete).
     */
    enum class BuddyOperation {
        ADDED, DELETED
    }

    /**
     * Represents the result of attempting to add a new buddy.
     *
     * @property BUDDYLIST_FULL The list is at capacity and cannot accept more buddies.
     * @property ALREADY_ON_LIST The target character is already present in the list.
     * @property OK The buddy was successfully added.
     */
    enum class BuddyAddResult {
        BUDDYLIST_FULL, ALREADY_ON_LIST, OK
    }

    companion object : KLogging()
}