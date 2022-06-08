package net.server.channel.handlers

import client.Character
import client.Client
import mu.KLogging
import net.AbstractPacketHandler
import net.server.Server
import net.server.guild.Guild
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket
import tools.packet.GuildPacket
import tools.packet.InteractPacket

class GuildOperationHandler : AbstractPacketHandler() {
    private val invited = mutableListOf<Invited>()
    private var nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000

    private fun isGuildNameAcceptable(name: String): Boolean {
        val bytes = name.encodeToByteArray()
        return bytes.size in 4..12
    }

    private fun respawnPlayer(mc: Character) {
        mc.map.broadcastMessage(mc, GameplayPacket.removePlayerFromMap(mc.id), false)
        mc.map.broadcastMessage(mc, GameplayPacket.spawnPlayerMapObject(mc), false)
        mc.updatePartyMemberHp()
    }

    private class Invited(rawName: String, val gid: Int) {
        val name = rawName.lowercase()
        val expiration = System.currentTimeMillis() + 60 * 60 * 1000
    }

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        if (System.currentTimeMillis() >= nextPruneTime) {
            invited.removeIf { System.currentTimeMillis() >= it.expiration }
            nextPruneTime = System.currentTimeMillis() + 20 * 60 * 1000
        }
        val mc = c.player ?: return
        val type = slea.readByte()
        when (type.toInt()) {
            0x00 -> c.announce(GuildPacket.showGuildInfo(mc))
            0x02 -> {
                if (mc.guildId > 0 || mc.mapId != 200000301) {
                    c.player?.dropMessage(1, "You cannot create a new Guild while in one.")
                    return
                }
                if (mc.meso.get() < Guild.CREATE_GUILD_COST) {
                    c.player?.dropMessage(1, "You do not have enough mesos to create a Guild.")
                    return
                }
                val guildName = slea.readGameASCIIString()
                if (!isGuildNameAcceptable(guildName)) {
                    c.player?.dropMessage(1, "The Guild name you have chosen is not accepted.")
                    return
                }
                val gid = Guild.createGuild(mc.id, guildName)
                if (gid == 0) {
                    c.announce(GuildPacket.genericGuildMessage(0x1c))
                    return
                }
                mc.gainMeso(-Guild.CREATE_GUILD_COST, show = true, enableActions = false, inChat = true)
                mc.guildId = gid
                mc.guildRank = 1
                mc.saveGuildStatus()
                c.announce(GuildPacket.showGuildInfo(mc))
                c.player?.dropMessage(1, "You have successfully created a Guild.")
                respawnPlayer(mc)
            }
            0x05 -> {
                if (mc.guildId <= 0 || mc.guildRank > 2) return
                val name = slea.readGameASCIIString()
                val mgr = Guild.sendInvite(c, name)
                if (mgr != null) {
                    c.announce(mgr.getPacket())
                } else {
                    val inv = Invited(name, mc.guildId)
                    if (!invited.contains(inv)) invited.add(inv)
                }
            }
            0x06 -> {
                if (mc.guildId > 0) {
                    logger.warn { "${mc.name} attempted to join a guild when s/he is already in one." }
                    return
                }
                val gid = slea.readInt()
                val cid = slea.readInt()
                if (cid != mc.id) {
                    logger.warn { "${mc.name} attempted to join a guild with a different character id." }
                }
                val name = mc.name.lowercase()
                val itr = invited.iterator()
                val isOnList = !(invited.removeIf { it.gid == gid && it.name == name })
                if (!isOnList) {
                    logger.warn { "${mc.name} is trying to join a guild that never invited him/her (or that the invitation has expired." }
                    return
                }
                mc.guildId = gid
                mc.guildRank = 5
                val s = mc.mgc?.let { Server.addGuildMember(it) } ?: 0
                if (s == 0) {
                    c.player?.dropMessage(1, "가입하려는 길드는 이미 정원이 꽉 찼습니다.")
                    mc.guildId = 0
                    return
                }
                c.announce(GuildPacket.showGuildInfo(mc))
                mc.saveGuildStatus()
                respawnPlayer(mc)
            }
            0x07 -> {
                val cid = slea.readInt()
                val name = slea.readGameASCIIString()
                if (cid != mc.id || name != mc.name || mc.guildId <= 0) {
                    logger.warn { "${mc.name} tried to quit guild under the name $name and current guild id of ${mc.guildId}." }
                    return
                }
                mc.mgc?.let { Server.getGuild(it)?.leaveGuild(it) }
                c.announce(GuildPacket.showGuildInfo(null))
                mc.guildId = 0
                mc.saveGuildStatus()
                respawnPlayer(mc)
            }
            0x08 -> {
                val cid = slea.readInt()
                val name = slea.readGameASCIIString()
                if (mc.guildRank > 2 || mc.guildId <= 0) {
                    logger.warn { "${mc.name} is trying to expel without rank 1 or 2." }
                    return
                }
                val mgc = mc.mgc ?: return
                Server.getGuild(mgc)?.expelMember(mgc, name, cid)
            }
            0x0d -> {
                if (mc.guildId <= 0 || mc.guildRank != 1) {
                    logger.warn { "${mc.name} tried to change guild rank titles when s/he does not have permission." }
                    return
                }
                val ranks = mutableListOf<String>()
                for (i in 0..4) {
                    ranks[i] = slea.readGameASCIIString()
                }
                Server.getGuild(mc.guildId)?.changeRankTitle(ranks.toTypedArray())
            }
            0x0e -> {
                val cid = slea.readInt()
                val newRank = slea.readByte()
                if (mc.guildRank > 2 || (newRank <= 2 && mc.guildRank != 1) || mc.guildId <= 0) {
                    logger.warn { "${mc.name} is trying to change rank outside of his/her permissions." }
                    return
                }
                if (newRank <= 1 || newRank > 5) return
                Server.getGuild(mc.guildId)?.changeRank(cid, newRank.toInt())
            }
            0x0f -> {
                if (mc.guildId <= 0 || mc.guildRank != 1 || mc.mapId != 200000301) {
                    logger.warn { "${mc.name} tried to change guild emblem without being the guild leader." }
                    return
                }
                if (mc.meso.get() < Guild.CHANGE_EMBLEM_COST) {
                    c.announce(InteractPacket.serverNotice(1, "You do not have enough mesos to create a Guild."))
                    return
                }
                val bg = slea.readShort()
                val bgColor = slea.readByte()
                val logo = slea.readShort()
                val logoColor = slea.readByte()
                Server.getGuild(mc.guildId)?.setGuildEmblem(bg, bgColor, logo, logoColor)
                respawnPlayer(mc)
            }
            0x10 -> {
                if (mc.guildId <= 0 || mc.guildRank > 2) {
                    logger.warn { "${mc.name} tried to change guild notice while not in a guild." }
                    return
                }
                val notice = slea.readGameASCIIString()
                if (notice.length > 100) return
                Server.getGuild(mc.guildId)?.setGuildNotice(notice)
            }
            else -> {
                logger.error { "Unhandled GUILD_OPERATION packet: \n $slea" }
            }
        }
    }

    companion object : KLogging()
}