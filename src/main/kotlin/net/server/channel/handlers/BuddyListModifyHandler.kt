package net.server.channel.handlers

import client.BuddyList
import client.BuddyListEntry
import client.CharacterNameAndId
import client.Client
import database.Buddies
import database.Characters
import mu.KLogging
import net.AbstractPacketHandler
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket
import java.sql.SQLException

class BuddyListModifyHandler : AbstractPacketHandler() {
    private fun getCharacterIdAndNameFromDatabase(name: String): CharacterIdNameBuddyCapacity? {
        var ret: CharacterIdNameBuddyCapacity? = null
        try {
            transaction {
                val row = Characters.select(Characters.id, Characters.name, Characters.buddyCapacity).where { Characters.name like name }
                if (!row.empty()) {
                    val it = row.first()
                    ret = CharacterIdNameBuddyCapacity(it[Characters.id], it[Characters.name], it[Characters.buddyCapacity])
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to fetch character id and name." }
        }
        return ret
    }

    private fun nextPendingRequest(c: Client) {
        c.player?.let { player ->
            val pendingBuddyRequest = player.buddyList.pollPendingRequest()
            pendingBuddyRequest.let {
                c.announce(InteractPacket.requestBuddyListAdd(it.id, player.id, pendingBuddyRequest.name))
            }
        }
    }

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val mode = slea.readByte()
        val player = c.player ?: return
        val buddyList = player.buddyList
        when (mode.toInt()) {
            1 -> {
                val addName = slea.readGameASCIIString()
                if (addName.length < 2 || addName.length > 13) return
                val ble = buddyList.getEntry(addName)
                if (ble != null && !ble.visible) {
                    c.announce(InteractPacket.serverNotice(1, "이미 친구로 등록되어 있습니다."))
                } else if (buddyList.isFull() && ble == null) {
                    c.announce(InteractPacket.serverNotice(1, "친구리스트가 가득 찼습니다."))
                } else if (ble == null) {
                    try {
                        val world = c.getWorldServer()
                        val otherChar = c.getChannelServer().players.getCharacterByName(addName)
                        val charWithId: CharacterIdNameBuddyCapacity?
                        val channel: Int
                        if (otherChar != null) {
                            channel = c.channel
                            charWithId = CharacterIdNameBuddyCapacity(otherChar.id, otherChar.name, otherChar.buddyList.capacity)
                        } else {
                            channel = world.findChannelIdByCharacterName(addName)
                            charWithId = getCharacterIdAndNameFromDatabase(addName)
                        }
                        if (charWithId != null) {
                            var buddyAddResult: BuddyList.BuddyAddResult = BuddyList.BuddyAddResult.BUDDYLIST_FULL
                            if (channel != -1) {
                                buddyAddResult = world.requestBuddyAdd(addName, c.channel, player.id, player.name)
                            } else {
                                transaction {
                                    val cnt = Buddies.select(Buddies.id).where { (Buddies.characterId eq charWithId.id) and (Buddies.pending eq 0) }.count()
                                    if (cnt >= charWithId.buddyCapacity) {
                                        buddyAddResult = BuddyList.BuddyAddResult.BUDDYLIST_FULL
                                    }
                                    val row = Buddies.select(Buddies.pending).where {
                                        (Buddies.characterId eq charWithId.id) eq (Buddies.buddyId eq player.id)
                                    }
                                    if (!row.empty()) {
                                        buddyAddResult = BuddyList.BuddyAddResult.ALREADY_ON_LIST
                                    }
                                }
                            }
                            if (buddyAddResult == BuddyList.BuddyAddResult.BUDDYLIST_FULL) {
                                c.announce(InteractPacket.serverNotice(1, "상대 친구목록이 가득 찼습니다."))
                            } else {
                                var displayChannel = -1
                                val otherCid = charWithId.id
                                if (buddyAddResult == BuddyList.BuddyAddResult.ALREADY_ON_LIST && channel != -1) {
                                    displayChannel = channel
                                    notifyRemoteChannel(c, channel, otherCid, BuddyList.BuddyOperation.ADDED)
                                } else if (buddyAddResult != BuddyList.BuddyAddResult.ALREADY_ON_LIST && channel == -1) {
                                    transaction {
                                        Buddies.insert {
                                            it[characterId] = charWithId.id
                                            it[buddyId] = player.id
                                            it[pending] = 1
                                        }
                                    }
                                }
                                buddyList.addEntry(BuddyListEntry(charWithId.name, "그룹 미지정", otherCid, displayChannel, true))
                                c.announce(InteractPacket.updateBuddyList(buddyList.buddies.values))
                            }
                        } else {
                            c.announce(InteractPacket.serverNotice(1, "대상을 발견하지 못했습니다."))
                        }
                    } catch (e: SQLException) {
                        logger.error(e) { "Failed to handle buddy list modify handler. "}
                    }
                }
            }
            2 -> { // Accept buddy
                val otherCid = slea.readInt()
                if (!buddyList.isFull()) {
                    try {
                        val channel = c.getWorldServer().findChannelIdByCharacterId(otherCid)
                        val otherChar = c.getChannelServer().players.getCharacterById(otherCid)
                        var otherName: String? = null
                        if (otherChar == null) {
                            transaction {
                                val row = Characters.select(Characters.name).where { Characters.id eq otherCid }
                                if (!row.empty()) {
                                    otherName = row.first()[Characters.name]
                                }
                            }
                        } else {
                            otherName = otherChar.name
                        }
                        if (otherName != null) {
                            buddyList.addEntry(BuddyListEntry(otherName, "그룹 미지정", otherCid, channel, true))
                            c.announce(InteractPacket.updateBuddyList(buddyList.buddies.values))
                            notifyRemoteChannel(c, channel, otherCid, BuddyList.BuddyOperation.ADDED)
                        }
                    } catch (e: SQLException) {
                        logger.error(e) { "Failed to handle buddy accept request." }
                    }
                }
            }
            3 -> { // Delete
                val otherCid = slea.readInt()
                if (buddyList.containsVisible(otherCid)) {
                    notifyRemoteChannel(c, c.getWorldServer().findChannelIdByCharacterId(otherCid), otherCid, BuddyList.BuddyOperation.DELETED)
                }
                buddyList.remove(otherCid)
                c.announce(InteractPacket.updateBuddyList(player.buddyList.buddies.values))
                nextPendingRequest(c)
            }
            else -> {}
        }
    }

    private fun notifyRemoteChannel(c: Client, remoteChannel: Int, otherCid: Int, operation: BuddyList.BuddyOperation) {
        if (remoteChannel != -1) {
            c.player?.let {
                c.getWorldServer().buddyChanged(otherCid, it.id, it.name, c.channel, operation)
            }
        }
    }

    companion object : KLogging() {
        class CharacterIdNameBuddyCapacity(id: Int, name: String, val buddyCapacity: Int) : CharacterNameAndId(id, name)
    }
}