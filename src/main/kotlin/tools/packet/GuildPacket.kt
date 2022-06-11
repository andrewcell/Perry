package tools.packet

import client.Character
import database.BBSReplies
import database.BBSReplies.content
import database.BBSReplies.replyId
import database.BBSThreads.icon
import database.BBSThreads.localThreadId
import database.BBSThreads.name
import database.BBSThreads.posterCid
import database.BBSThreads.replyCount
import database.BBSThreads.startPost
import database.BBSThreads.timestamp
import database.Guilds
import net.SendPacketOpcode
import net.server.guild.Guild
import net.server.guild.GuildCharacter
import org.jetbrains.exposed.sql.ResultRow
import tools.PacketCreator
import tools.data.output.PacketLittleEndianWriter
import java.sql.SQLException
import kotlin.math.min

class GuildPacket {
    companion object {
        @Throws(SQLException::class)
        fun addThread(lew: PacketLittleEndianWriter, rs: ResultRow?) {
            if (rs == null) return
            lew.int(rs[localThreadId])
            lew.int(rs[posterCid])
            lew.gameASCIIString(rs[name])
            lew.long(PacketCreator.getTime(rs[timestamp].toEpochMilli()))
            lew.int(rs[icon].toInt())
            lew.int(rs[replyCount].toInt())
        }

        @Throws(SQLException::class)
        fun guildBBSThreadList(rs: List<ResultRow?>, startNumber: Int): ByteArray {
            var start = startNumber
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_BBS_PACKET.value)
            lew.byte(0x06)
            if (rs.isEmpty()) {
                lew.byte(0)
                lew.int(0)
                lew.int(0)
                return lew.getPacket()
            }
            var threadCount = rs.size
            if ((rs[rs.size - 1]?.get(localThreadId)) == 0) { //has a notice
                lew.byte(1)
                addThread(lew, rs[rs.size - 1])
                threadCount-- //one thread didn't count (because it's a notice)
            } else {
                lew.byte(0)
            }
            if (rs[start + 1] == null) {
                start = 0
            }
            lew.int(threadCount)
            lew.int(min(10, threadCount - start))
            for (i in 0 until min(10, threadCount - start)) {
                addThread(lew, rs[i])
            }
            return lew.getPacket()
        }

        //rank change
        fun guildCapacityChange(gid: Int, capacity: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x3A)
            lew.int(gid)
            lew.byte(capacity)
            return lew.getPacket()
        }

        fun guildDisband(gid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x32)
            lew.int(gid)
            lew.byte(1)
            return lew.getPacket()
        }

        fun guildEmblemChange(gid: Int, bg: Short, bgColor: Byte, logo: Short, logoColor: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x42)
            lew.int(gid)
            lew.short(bg.toInt())
            lew.byte(bgColor)
            lew.short(logo.toInt())
            lew.byte(logoColor)
            return lew.getPacket()
        }

        fun guildInvite(gid: Int, charName: String?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x05)
            lew.int(gid)
            lew.gameASCIIString(charName!!)
            return lew.getPacket()
        }

        fun guildNotice(gid: Int, notice: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x44)
            lew.int(gid)
            lew.gameASCIIString(notice)
            return lew.getPacket()
        }

        //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
        fun guildMemberLeft(mgc: GuildCharacter, bExpelled: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(if (bExpelled) 0x2f else 0x2c)
            lew.int(mgc.guildId)
            lew.int(mgc.id)
            lew.gameASCIIString(mgc.name)
            return lew.getPacket()
        }

        //rank change
        fun guildMemberLevelJobUpdate(mgc: GuildCharacter): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x3C)
            lew.int(mgc.guildId)
            lew.int(mgc.id)
            lew.int(mgc.level)
            lew.int(mgc.jobId)
            return lew.getPacket()
        }

        fun guildMemberOnline(gid: Int, cid: Int, bOnline: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x3d)
            lew.int(gid)
            lew.int(cid)
            lew.byte(if (bOnline) 1 else 0)
            return lew.getPacket()
        }

        /**
         * Sends a "job advance" packet to the guild or family.
         *
         *
         * Possible values for
         * `type`:<br></br> 0: <Guild ? has advanced to a(an) ?.></Guild><br></br> 1:
         * <Family ? has advanced to a(an) ?.></Family><br></br>
         *
         * @param type The type
         * @return The "job advance" packet.
         */
        fun jobMessageToGuild(type: Int, job: Int, charName: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.NOTIFY_JOB_CHANGE.value)
            lew.byte(type)
            lew.int(job)
            lew.gameASCIIString("> $charName")
            return lew.getPacket()
        }

        /**
         * Sends a "levelup" packet to the guild or family.
         *
         *
         * Possible values for
         * `type`:<br></br> 0: <Family> ? has reached Lv. ?.<br></br> - The Reps
         * you have received from ? will be reduced in half. 1: <Family> ? has
         * reached Lv. ?.<br></br> 2: <Guild> ? has reached Lv. ?.<br></br>
         *
         * @param type The type
         * @return The "levelup" packet.
        </Guild></Family></Family> */
        fun levelUpMessageToGuild(type: Int, level: Int, charName: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.NOTIFY_LEVELUP.value)
            lew.byte(type)
            lew.int(level)
            lew.gameASCIIString(charName)
            return lew.getPacket()
        }

        fun newGuildMember(mgc: GuildCharacter): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x27)
            lew.int(mgc.guildId)
            lew.int(mgc.id)
            lew.ASCIIString(mgc.name, 13)
            lew.int(mgc.jobId)
            lew.int(mgc.level)
            lew.int(mgc.guildRank) //should be always 5 but whatevs
            lew.int(if (mgc.online) 1 else 0) //should always be 1 too
            lew.int(1) //? could be guild signature, but doesn't seem to matter
            lew.int(3)
            return lew.getPacket()
        }

        //rank change
        fun rankTitleChange(gid: Int, ranks: Array<String>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x3E)
            lew.int(gid)
            for (i in 0..4) {
                lew.gameASCIIString(ranks[i])
            }
            return lew.getPacket()
        }

        fun showGuildInfo(c: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x1A) //signature for showing guild info
            if (c == null) { //show empty guild (used for leaving, expelled)
                lew.byte(0)
                return lew.getPacket()
            }
            val g = c.client.getWorldServer().getGuild(c.mgc)
            if (g == null) { //failed to read from DB - don't show a guild
                lew.byte(0)
                return lew.getPacket()
            } else {
                c.guildRank = c.guildRank
            }
            lew.byte(1) //bInGuild
            lew.int(g.id)
            lew.gameASCIIString(g.name)
            for (i in 1..5) {
                lew.gameASCIIString(g.getRankTitle(i))
            }
            val members: Collection<GuildCharacter> = g.members
            lew.byte(members.size) //then it is the size of all the members
            for (mgc in members) { //and each of their character ids o_O
                lew.int(mgc.id)
            }
            for (mgc in members) {
                lew.ASCIIString(mgc.name, 13)
                lew.int(mgc.jobId)
                lew.int(mgc.level)
                lew.int(mgc.guildRank)
                lew.int(if (mgc.online) 1 else 0)
                lew.int(g.signature)
            }
            lew.int(g.capacity)
            lew.short(g.logoBG.toInt())
            lew.byte(g.logoBGColor)
            lew.short(g.logo.toInt())
            lew.byte(g.logoColor)
            lew.gameASCIIString(g.notice)
            lew.int(g.gp)
            return lew.getPacket()
        }

        @Throws(SQLException::class)
        fun showGuildRanks(npcId: Int, rs: List<ResultRow>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x49)
            lew.int(npcId)
            if (rs.isEmpty()) { //no guilds
                lew.int(0)
                return lew.getPacket()
            }
            lew.int(rs.size) //number of entries
            for (row in rs) {
                lew.gameASCIIString(row[Guilds.name])
                lew.int(row[Guilds.GP])
                lew.int(row[Guilds.logo])
                lew.int(row[Guilds.logoColor].toInt())
                lew.int(row[Guilds.logoBG])
                lew.int(row[Guilds.logoBGColor].toInt())
            }
            return lew.getPacket()
        }

        @Throws(SQLException::class, RuntimeException::class)
        fun showThread(localThreadId: Int, threadRS: ResultRow, repliesRS: List<ResultRow>?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_BBS_PACKET.value)
            lew.byte(0x07)
            lew.int(localThreadId)
            lew.int(threadRS[posterCid])
            lew.long(PacketCreator.getTime(threadRS[timestamp].toEpochMilli()))
            lew.gameASCIIString(threadRS[name])
            lew.gameASCIIString(threadRS[startPost])
            lew.int(threadRS[icon].toInt())
            if (repliesRS != null) {
                val replyCount = threadRS[replyCount].toInt()
                lew.int(replyCount)
                var i = 0
                while (i < replyCount) {
                    lew.int(repliesRS[i][replyId])
                    lew.int(repliesRS[i][BBSReplies.posterCid])
                    lew.long(PacketCreator.getTime(repliesRS[i][BBSReplies.timestamp].toEpochMilli()))
                    lew.gameASCIIString(repliesRS[i][content])
                    i++
                }
                /*if (i != replyCount || repliesRS.next()) {
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
            }*/
            } else {
                lew.int(0)
            }
            return lew.getPacket()
        }

        fun changeGuildRank(mgc: GuildCharacter): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x40)
            lew.int(mgc.guildId)
            lew.int(mgc.id)
            lew.byte(mgc.guildRank)
            return lew.getPacket()
        }

        /**
         * 'Char' has denied your guild invitation.
         *
         * @param charName
         * @return
         */
        fun denyGuildInvitation(charName: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(0x37)
            lew.gameASCIIString(charName)
            return lew.getPacket()
        }

        fun genericGuildMessage(code: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GUILD_OPERATION.value)
            lew.byte(code)
            return lew.getPacket()
        }

        private fun getGuildInfo(lew: PacketLittleEndianWriter, guild: Guild) {
            lew.int(guild.id)
            lew.gameASCIIString(guild.name)
            for (i in 1..5) {
                lew.gameASCIIString(guild.getRankTitle(i))
            }
            val members = guild.members
            lew.byte(members.size)
            for (mgc in members) {
                lew.int(mgc.id)
            }
            for (mgc in members) {
                lew.ASCIIString(mgc.name, 13)
                lew.int(mgc.jobId)
                lew.int(mgc.level)
                lew.int(mgc.guildRank)
                lew.int(if (mgc.online) 1 else 0)
                lew.int(guild.signature)
            }
            lew.int(guild.capacity)
            lew.short(guild.logoBG.toInt())
            lew.byte(guild.logoBGColor.toInt())
            lew.short(guild.logo.toInt())
            lew.byte(guild.logoColor.toInt())
            lew.gameASCIIString(guild.notice)
            lew.int(guild.gp)
        }
    }
}