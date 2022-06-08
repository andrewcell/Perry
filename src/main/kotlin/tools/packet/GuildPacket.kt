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
            lew.writeInt(rs[localThreadId])
            lew.writeInt(rs[posterCid])
            lew.writeGameASCIIString(rs[name])
            lew.writeLong(PacketCreator.getTime(rs[timestamp].toEpochMilli()))
            lew.writeInt(rs[icon].toInt())
            lew.writeInt(rs[replyCount].toInt())
        }

        @Throws(SQLException::class)
        fun guildBBSThreadList(rs: List<ResultRow?>, startNumber: Int): ByteArray {
            var start = startNumber
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_BBS_PACKET.value)
            lew.write(0x06)
            if (rs.isEmpty()) {
                lew.write(0)
                lew.writeInt(0)
                lew.writeInt(0)
                return lew.getPacket()
            }
            var threadCount = rs.size
            if ((rs[rs.size - 1]?.get(localThreadId)) == 0) { //has a notice
                lew.write(1)
                addThread(lew, rs[rs.size - 1])
                threadCount-- //one thread didn't count (because it's a notice)
            } else {
                lew.write(0)
            }
            if (rs[start + 1] == null) {
                start = 0
            }
            lew.writeInt(threadCount)
            lew.writeInt(min(10, threadCount - start))
            for (i in 0 until min(10, threadCount - start)) {
                addThread(lew, rs[i])
            }
            return lew.getPacket()
        }

        //rank change
        fun guildCapacityChange(gid: Int, capacity: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x3A)
            lew.writeInt(gid)
            lew.write(capacity)
            return lew.getPacket()
        }

        fun guildDisband(gid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x32)
            lew.writeInt(gid)
            lew.write(1)
            return lew.getPacket()
        }

        fun guildEmblemChange(gid: Int, bg: Short, bgColor: Byte, logo: Short, logoColor: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x42)
            lew.writeInt(gid)
            lew.writeShort(bg.toInt())
            lew.write(bgColor)
            lew.writeShort(logo.toInt())
            lew.write(logoColor)
            return lew.getPacket()
        }

        fun guildInvite(gid: Int, charName: String?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x05)
            lew.writeInt(gid)
            lew.writeGameASCIIString(charName!!)
            return lew.getPacket()
        }

        fun guildNotice(gid: Int, notice: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x44)
            lew.writeInt(gid)
            lew.writeGameASCIIString(notice)
            return lew.getPacket()
        }

        //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
        fun guildMemberLeft(mgc: GuildCharacter, bExpelled: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(if (bExpelled) 0x2f else 0x2c)
            lew.writeInt(mgc.guildId)
            lew.writeInt(mgc.id)
            lew.writeGameASCIIString(mgc.name)
            return lew.getPacket()
        }

        //rank change
        fun guildMemberLevelJobUpdate(mgc: GuildCharacter): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x3C)
            lew.writeInt(mgc.guildId)
            lew.writeInt(mgc.id)
            lew.writeInt(mgc.level)
            lew.writeInt(mgc.jobId)
            return lew.getPacket()
        }

        fun guildMemberOnline(gid: Int, cid: Int, bOnline: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x3d)
            lew.writeInt(gid)
            lew.writeInt(cid)
            lew.write(if (bOnline) 1 else 0)
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
            lew.write(SendPacketOpcode.NOTIFY_JOB_CHANGE.value)
            lew.write(type)
            lew.writeInt(job)
            lew.writeGameASCIIString("> $charName")
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
            lew.write(SendPacketOpcode.NOTIFY_LEVELUP.value)
            lew.write(type)
            lew.writeInt(level)
            lew.writeGameASCIIString(charName)
            return lew.getPacket()
        }

        fun newGuildMember(mgc: GuildCharacter): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x27)
            lew.writeInt(mgc.guildId)
            lew.writeInt(mgc.id)
            lew.writeASCIIString(mgc.name, 13)
            lew.writeInt(mgc.jobId)
            lew.writeInt(mgc.level)
            lew.writeInt(mgc.guildRank) //should be always 5 but whatevs
            lew.writeInt(if (mgc.online) 1 else 0) //should always be 1 too
            lew.writeInt(1) //? could be guild signature, but doesn't seem to matter
            lew.writeInt(3)
            return lew.getPacket()
        }

        //rank change
        fun rankTitleChange(gid: Int, ranks: Array<String>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x3E)
            lew.writeInt(gid)
            for (i in 0..4) {
                lew.writeGameASCIIString(ranks[i])
            }
            return lew.getPacket()
        }

        fun showGuildInfo(c: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x1A) //signature for showing guild info
            if (c == null) { //show empty guild (used for leaving, expelled)
                lew.write(0)
                return lew.getPacket()
            }
            val g = c.client.getWorldServer().getGuild(c.mgc)
            if (g == null) { //failed to read from DB - don't show a guild
                lew.write(0)
                return lew.getPacket()
            } else {
                c.guildRank = c.guildRank
            }
            lew.write(1) //bInGuild
            lew.writeInt(g.id)
            lew.writeGameASCIIString(g.name)
            for (i in 1..5) {
                lew.writeGameASCIIString(g.getRankTitle(i))
            }
            val members: Collection<GuildCharacter> = g.members
            lew.write(members.size) //then it is the size of all the members
            for (mgc in members) { //and each of their character ids o_O
                lew.writeInt(mgc.id)
            }
            for (mgc in members) {
                lew.writeASCIIString(mgc.name, 13)
                lew.writeInt(mgc.jobId)
                lew.writeInt(mgc.level)
                lew.writeInt(mgc.guildRank)
                lew.writeInt(if (mgc.online) 1 else 0)
                lew.writeInt(g.signature)
            }
            lew.writeInt(g.capacity)
            lew.writeShort(g.logoBG.toInt())
            lew.write(g.logoBGColor)
            lew.writeShort(g.logo.toInt())
            lew.write(g.logoColor)
            lew.writeGameASCIIString(g.notice)
            lew.writeInt(g.gp)
            return lew.getPacket()
        }

        @Throws(SQLException::class)
        fun showGuildRanks(npcId: Int, rs: List<ResultRow>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x49)
            lew.writeInt(npcId)
            if (rs.isEmpty()) { //no guilds
                lew.writeInt(0)
                return lew.getPacket()
            }
            lew.writeInt(rs.size) //number of entries
            for (row in rs) {
                lew.writeGameASCIIString(row[Guilds.name])
                lew.writeInt(row[Guilds.GP])
                lew.writeInt(row[Guilds.logo])
                lew.writeInt(row[Guilds.logoColor].toInt())
                lew.writeInt(row[Guilds.logoBG])
                lew.writeInt(row[Guilds.logoBGColor].toInt())
            }
            return lew.getPacket()
        }

        @Throws(SQLException::class, RuntimeException::class)
        fun showThread(localThreadId: Int, threadRS: ResultRow, repliesRS: List<ResultRow>?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_BBS_PACKET.value)
            lew.write(0x07)
            lew.writeInt(localThreadId)
            lew.writeInt(threadRS[posterCid])
            lew.writeLong(PacketCreator.getTime(threadRS[timestamp].toEpochMilli()))
            lew.writeGameASCIIString(threadRS[name])
            lew.writeGameASCIIString(threadRS[startPost])
            lew.writeInt(threadRS[icon].toInt())
            if (repliesRS != null) {
                val replyCount = threadRS[replyCount].toInt()
                lew.writeInt(replyCount)
                var i = 0
                while (i < replyCount) {
                    lew.writeInt(repliesRS[i][replyId])
                    lew.writeInt(repliesRS[i][BBSReplies.posterCid])
                    lew.writeLong(PacketCreator.getTime(repliesRS[i][BBSReplies.timestamp].toEpochMilli()))
                    lew.writeGameASCIIString(repliesRS[i][content])
                    i++
                }
                /*if (i != replyCount || repliesRS.next()) {
                throw new RuntimeException(String.valueOf(threadRS.getInt("threadid")));
            }*/
            } else {
                lew.writeInt(0)
            }
            return lew.getPacket()
        }

        fun changeGuildRank(mgc: GuildCharacter): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x40)
            lew.writeInt(mgc.guildId)
            lew.writeInt(mgc.id)
            lew.write(mgc.guildRank)
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
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(0x37)
            lew.writeGameASCIIString(charName)
            return lew.getPacket()
        }

        fun genericGuildMessage(code: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GUILD_OPERATION.value)
            lew.write(code)
            return lew.getPacket()
        }

        private fun getGuildInfo(lew: PacketLittleEndianWriter, guild: Guild) {
            lew.writeInt(guild.id)
            lew.writeGameASCIIString(guild.name)
            for (i in 1..5) {
                lew.writeGameASCIIString(guild.getRankTitle(i))
            }
            val members = guild.members
            lew.write(members.size)
            for (mgc in members) {
                lew.writeInt(mgc.id)
            }
            for (mgc in members) {
                lew.writeASCIIString(mgc.name, 13)
                lew.writeInt(mgc.jobId)
                lew.writeInt(mgc.level)
                lew.writeInt(mgc.guildRank)
                lew.writeInt(if (mgc.online) 1 else 0)
                lew.writeInt(guild.signature)
            }
            lew.writeInt(guild.capacity)
            lew.writeShort(guild.logoBG.toInt())
            lew.write(guild.logoBGColor.toInt())
            lew.writeShort(guild.logo.toInt())
            lew.write(guild.logoColor.toInt())
            lew.writeGameASCIIString(guild.notice)
            lew.writeInt(guild.gp)
        }
    }
}