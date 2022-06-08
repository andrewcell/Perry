package client

import database.Buddies
import database.Characters
import mu.KLogging
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tools.packet.InteractPacket
import java.sql.SQLException

class BuddyList(var capacity: Int) {
    val buddies = mutableMapOf<Int, BuddyListEntry>()
    val pendingRequests = mutableListOf<CharacterNameAndId>()

    fun contains(characterId: Int) = buddies.containsKey(characterId)

    fun containsVisible(characterId: Int) = buddies[characterId]?.visible ?: false

    fun getEntry(cId: Int) = buddies[cId]

    fun getEntry(name: String) = buddies.values.find { it.name == name.lowercase() }

    fun addEntry(e: BuddyListEntry) = buddies.put(e.characterId, e)

    fun remove(characterId: Int) = buddies.remove(characterId)

    fun isFull() = buddies.size >= capacity

    fun getBuddyIds() = buddies.keys.toList()

    fun loadFromDatabase(characterId: Int) {
        try {
            transaction {
                (Buddies innerJoin Characters)
                    .slice(Buddies.buddyId, Buddies.pending, Buddies.group, Characters.name)
                    .select {
                        (Characters.id eq characterId) and (Buddies.buddyId eq Characters.id) }
                    .forEach {
                    if (it[Buddies.pending].toInt() == 1) {
                        pendingRequests.add(CharacterNameAndId(it[Buddies.buddyId], it[Characters.name]))
                    } else {
                        addEntry(BuddyListEntry(it[Characters.name], it[Buddies.group], it[Buddies.buddyId], 1, true))
                    }
                }
                Buddies.deleteWhere { (Buddies.pending eq 1) and (Buddies.characterId eq characterId) }
            }
        } catch (e: SQLException) {
            logger.error (e) { "Failed to load Buddy list from database." }
        }
    }

    fun pollPendingRequest() = pendingRequests.removeLast()

    fun addBuddyRequest(c: Client, cidFrom: Int, nameFrom: String, channelFrom: Int) {
        addEntry(BuddyListEntry(nameFrom, "그룹 미지정", cidFrom, channelFrom, false))
        if (pendingRequests.isEmpty()) {
            c.player?.let { c.announce(InteractPacket.requestBuddyListAdd(cidFrom, it.id, nameFrom)) }
        } else {
            pendingRequests.add(CharacterNameAndId(cidFrom, nameFrom))
        }
    }

    enum class BuddyOperation {
        ADDED, DELETED
    }

    enum class BuddyAddResult {
        BUDDYLIST_FULL, ALREADY_ON_LIST, OK
    }

    companion object : KLogging()
}
