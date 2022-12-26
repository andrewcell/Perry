package net.server.channel.handlers

import client.Client
import database.BBSReplies
import database.BBSThreads
import database.BBSThreads.replyCount
import mu.KLogging
import net.AbstractPacketHandler
import org.jetbrains.exposed.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GuildPacket
import java.sql.SQLException
import java.time.Instant

class BBSOperationHandler : AbstractPacketHandler() {
    private fun correctLength(input: String, maxSize: Int) = if (input.length > maxSize) input.substring(0, maxSize) else input

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.player?.let { player ->
            if (player.guildId < 1) return
            val mode = slea.readByte()
            var localThreadId = 0
            when (mode.toInt()) {
                0 -> {
                    val edit = slea.readByte().toInt() == 1
                    if (edit) localThreadId = slea.readInt()
                    val notice = slea.readByte().toInt() == 1
                    val title = correctLength(slea.readGameASCIIString(), 25)
                    val text = correctLength(slea.readGameASCIIString(), 600)
                    val icon = slea.readInt()
                    if (icon in 0x64..0x6a) {
                        if (player.getItemQuantity(5290000 + icon - 0x64, false) > 0) {
                            return
                        }
                    } else if (icon < 0 || icon > 3) return
                    if (edit) {
                        newBBSThread(c, title, text, icon, notice)
                    } else {
                        editBBSThread(c, title, text, icon, localThreadId)
                    }
                }
                1 -> {
                    localThreadId = slea.readInt()
                    deleteBBSThread(c, localThreadId)
                }
                2 -> {
                    val start = slea.readInt()
                    listBBSThreads(c, start * 10)
                }
                3 -> { // List thread + reply, follewed by id (int)
                    localThreadId = slea.readInt()
                    displayThread(c, localThreadId)
                }
                4 -> { // reply
                    localThreadId = slea.readInt()
                    val text = correctLength(slea.readGameASCIIString(), 25)
                    newBBSReply(c, localThreadId, text)
                }
                5 -> { // delete reply
                    slea.readInt()
                    val replyId = slea.readInt()
                    deleteBBSReply(c, replyId)
                }
                else -> {
                    logger.warn { "Unknown BBS Operate code $mode." }
                }
            }
        }
    }

    companion object : KLogging() {
        fun deleteBBSReply(client: Client, replyId: Int) {
            val mc = client.player ?: return
            if (mc.guildId <= 0) return
            try {
                transaction {
                    val row = BBSReplies.select { BBSReplies.replyId eq replyId }
                    if (row.empty()) return@transaction
                    val reply = row.first()
                    if (mc.id != reply[BBSReplies.posterCid].toInt() && mc.guildRank > 2) return@transaction
                    val threadId = reply[BBSReplies.threadId]
                    BBSReplies.deleteWhere { BBSReplies.replyId eq replyId }
                    val thread = BBSThreads.select { BBSThreads.threadId eq threadId }
                    if (thread.empty()) return@transaction
                    BBSThreads.update({ BBSThreads.threadId eq threadId }) {
                        it[replyCount] = thread.first()[replyCount]
                    }
                    displayThread(client, threadId, false)
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to delete BBS reply from database." }
            }
        }

        fun deleteBBSThread(c: Client, localThreadId: Int) {
            val mc = c.player ?: return
            if (mc.guildId <= 0) return
            try {
                transaction {
                    val row = BBSThreads.select { (BBSThreads.guildId eq mc.guildId) and (BBSThreads.localThreadId eq localThreadId) }
                    if (row.empty()) return@transaction
                    val thread = row.first()
                    if (mc.id != thread[BBSThreads.posterCid] && mc.guildRank > 2) return@transaction
                    val threadId = thread[BBSThreads.threadId]
                    BBSReplies.deleteWhere { BBSReplies.threadId eq threadId }
                    BBSThreads.deleteWhere { BBSThreads.threadId eq threadId }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to delete BBS thread from database." }
            }
        }

        fun displayThread(client: Client, threadId: Int, isThreadIdLocal: Boolean = true) {
            val mc = client.player ?: return
            if (mc.guildId <= 0) return
            try {
                transaction {
                    val row =
                        BBSThreads.select { (BBSThreads.guildId eq mc.guildId) and ((if (isThreadIdLocal) BBSThreads.localThreadId else BBSThreads.threadId) eq threadId) }
                    if (row.empty()) return@transaction
                    val thread = row.first()
                    var bbsReplies = emptyList<ResultRow>()
                    if (thread[BBSThreads.replyCount] >= 0) {
                        bbsReplies =
                            BBSReplies.select { BBSReplies.threadId eq (if (!isThreadIdLocal) threadId else thread[BBSThreads.threadId]) }
                                .toList()
                    }
                    client.announce(
                        GuildPacket.showThread(
                            if (isThreadIdLocal) threadId else thread[BBSThreads.localThreadId],
                            thread,
                            bbsReplies
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to display BBS thread from database." }
            }
        }

        fun editBBSThread(c: Client, title: String, text: String, icon: Int, localThreadId: Int) { //TODO
          /*  val player = c.player ?: return
            if (player.guildId < 1) return
            try {
                transaction {
                    BBSThreads.update({
                        (BBSThreads.guildId eq player.guildId) and (BBSThreads.localThreadId eq localThreadId) and
                                ((BBSThreads.posterCid eq player.id))
                    })
                }
"UPDATE bbs_threads SET `name` = ?, `timestamp` = ?, `icon` = ?, `startpost` = ? WHERE guildid = ? AND localthreadid = ? AND (postercid = ? OR ?)")
                ps.setString(1, title)
                ps.setLong(2, System.currentTimeMillis())
                ps.setInt(3, icon)
                ps.setString(4, text)
                ps.setInt(5, player.guildId)
                ps.setInt(6, localThreadId)
                ps.setInt(7, player.id)
                ps.setBoolean(8, player.guildRank < 3)
                ps.execute()
                displayThread(c, localThreadId)
            } catch (e: SQLException) {
                log(e, "BBSOperationHandler")
            }*/
        }

        fun listBBSThreads(c: Client, start: Int) {
            val p = c.player ?: return
            try {
                transaction {
                    val list = BBSThreads.select { (BBSThreads.guildId eq p.guildId) }.orderBy(BBSThreads.localThreadId, SortOrder.DESC).toList()
                    c.announce(GuildPacket.guildBBSThreadList(list, start))
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to list BBS threads from database." }
            }
        }

        fun newBBSReply(c: Client, localThreadId: Int, text: String) {
            val player = c.player ?: return
            if (player.guildId <= 0) return
            try {
                transaction {
                    val thread = BBSThreads.select { (BBSThreads.guildId eq player.guildId) and (BBSThreads.localThreadId eq localThreadId) }
                    if (thread.empty()) return@transaction
                    val threadId = thread.first()[BBSThreads.threadId]
                    BBSReplies.insert {
                        it[BBSReplies.threadId] = threadId
                        it[posterCid] = player.id
                        it[timestamp] = Instant.now()
                        it[content] = text
                    }
                    BBSThreads.update({ BBSThreads.threadId eq threadId }) {
                        it[replyCount] = (thread.first()[replyCount] + 1).toShort()
                    }
                }
                displayThread(c, localThreadId)
            } catch (e: SQLException) {
                logger.error(e) { "Failed to create BBS reply from database." }
            }
        }

        fun newBBSThread(c: Client, title: String, text: String, icon: Int, notice: Boolean) {
            val player = c.player ?: return
            if (player.guildId <= 0) return
            var nextId = 0
            try {
                transaction {
                    if (!notice) {
                        nextId = BBSThreads.select { BBSThreads.guildId eq player.guildId }.maxOf { it[BBSThreads.localThreadId] }
                    }
                    BBSThreads.insert {
                        it[posterCid] = player.id
                        it[name] = title
                        it[timestamp] = Instant.now()
                        it[BBSThreads.icon] = icon.toShort()
                        it[startPost] = text
                        it[guildId] = player.guildRank
                        it[localThreadId] = nextId
                    }
                }
                displayThread(c, nextId)
            } catch (e: SQLException) {
                logger.error(e) { "Failed to create BBS thread from database." }
            }
        }
    }
}