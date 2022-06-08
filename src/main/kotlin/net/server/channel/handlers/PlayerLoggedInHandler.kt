package net.server.channel.handlers

import client.Character
import client.Client
import client.SkillFactory
import client.inventory.InventoryType
import database.DueyPackages
import mu.KLogging
import net.AbstractPacketHandler
import net.server.Server
import net.server.world.PartyOperation
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.*
import java.sql.SQLException

class PlayerLoggedInHandler : AbstractPacketHandler() {
    override fun validateState(c: Client) = !c.loggedIn

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val cid = slea.readInt()
        var player = c.getWorldServer().players.getCharacterById(cid)
        if (player == null) {
            player = Character.loadCharFromDatabase(cid, c, true)
        } else {
            player.newClient(c)
        }
        c.player = player
        player?.let { p ->
            c.accountId = p.accountId
            val state = c.getLoginState()
            var allowLogin = true
            val chServ = c.getChannelServer()
            if (state == Client.LOGIN_SERVER_TRANSITION || state == Client.LOGIN_NOTLOGGEDIN) {
                for (charName in c.loadCharacterNames(c.world)) {
                    c.getWorldServer().channels.forEach { ch ->
                        if (ch.isConnected(charName))
                            allowLogin = false
                    }
                    break
                }
            }
            if (state != Client.LOGIN_SERVER_TRANSITION || !allowLogin) {
                if (!player.channelCheck && player.cashShop?.opened == false) {
                    c.player = null
                    c.announce(LoginPacket.getAfterLoginError(7))
                    return
                }
            }
            player.channelCheck = false
            c.updateLoginState(Client.LOGIN_LOGGEDIN)
            chServ.addPlayer(player)
            val buffs = Server.buffStorage.getBuffsFromStorage(cid)
            player.silentGiveBuffs(buffs)
            try {
                transaction {
                    val row = DueyPackages.slice(DueyPackages.mesos).select { (DueyPackages.receiverId eq player.id) and (DueyPackages.checked eq true) }
                    if (!row.empty()) {
                        DueyPackages.update({ DueyPackages.receiverId eq player.id }) {
                            it[checked] = false
                        }
                        c.announce(PacketCreator.sendDuey(0x1b))
                    }
                }
            } catch (e: SQLException) {
                logger.warn(e) { "Failed to update Duey packages when handle log in." }
            }
            c.announce(CharacterPacket.getCharInfo(player))
            c.announce(CashPacket.enableCSUse())
            player.sendKeymap()
            player.map.addPlayer(player)
            val world = Server.getWorld(c.world)
            world.players.addPlayer(player)
            world.buddyLoggedOn(player.id, c.channel, player.buddyList)
            val buddyIds = player.buddyList.getBuddyIds()
            Server.getWorld(c.world).multiBuddyFind(player.id, buddyIds).forEach { onlineBuddy ->
                val ble = player.buddyList.getEntry(onlineBuddy.characterId)
                if (ble?.visible == true) {
                    ble.channel = onlineBuddy.channel
                    player.buddyList.addEntry(ble)
                }
            }
            c.announce(InteractPacket.updateBuddyList(player.buddyList.buddies.values))
            player.cashShop?.opened = false
            if (player.guildId > 0) {
                val playerGuild = player.mgc?.let { Server.getGuild(player.guildId, it) }
                if (playerGuild == null) {
                    player.deleteGuild(player.guildId)
                    player.mgc = null
                    player.guildId = 0
                } else {
                    player.mgc?.let { Server.setGuildMemberOnline(it, true, c.channel) }
                    c.announce(GuildPacket.showGuildInfo(player))
                }
            }
            player.showNote()
            player.mpc?.let { mpc ->
                mpc.channel = c.channel
                mpc.mapId = player.mapId
                mpc.online = true
                player.party?.id?.let { world.updateParty(it, PartyOperation.LOG_ONOFF, mpc) }
            }
            /*if (c.player?.haveItem(3993003, 1) == true) {
                c.player.loginExp2()
            } else c.player.loginExp()*/
            player.updatePartyMemberHp()
            if (player.getInventory(InventoryType.EQUIPPED)?.findById(1122017) != null) {
                player.equipPendantOfSpirit()
            }
            c.announce(CharacterPacket.updateGender(player))
            player.checkMessenger()
            c.announce(PacketCreator.enableReport())
            SkillFactory.getSkill(10000000 * player.getJobType() + 12)?.let { player.changeSkillLevel(it, ((player.linkedLevel / 10).toByte()), 20, -1) }
            player.checkBerserk()
            player.expirationTask()
            player.startMobHackParser()
            player.setRates()
            when (player.mapId) {
                190000000, 190000001, 190000002, 191000000, 191000001, 192000000, 192000001, 195000000, 195010000, 195020000, 195030000, 196000000, 196010000, 197000000, 197010000 -> {
                    player.changeMap(193000000, 0)
                    player.message("게임방으로 나왔습니다.")
                }
            }
            player.curse = false
            logger.info { "Account(${c.accountName}), User(${player.name}) is entered game. World: ${world.id}, Channel: ${chServ.channelId}" }
        }
    }

    companion object : KLogging()
}