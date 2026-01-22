package net.server.world

import client.BuddyList
import client.BuddyListEntry
import client.Character
import database.Characters
import mu.KLogging
import net.server.PlayerStorage
import net.server.Server
import net.server.channel.Channel
import net.server.channel.CharacterIdChannelPair
import net.server.guild.Guild
import net.server.guild.GuildCharacter
import net.server.guild.GuildSummary
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import tools.ServerJSON
import tools.packet.GameplayPacket
import tools.packet.GuildPacket
import tools.packet.InteractPacket
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicInteger

class World(val id: Int, var flag: Int, var eventMessage: String, var expRate: Int, var dropRate: Int, var mesoRate: Int, val bossDropRate: Int) {
    val channels = mutableListOf<Channel>()
    private val parties = mutableMapOf<Int, Party>()
    private val runningPartyId = AtomicInteger()
    private val messengers = mutableMapOf<Int, Messenger>()
    private val runningMessengerId = AtomicInteger()
    private val gsStore = mutableMapOf<Int, GuildSummary>()
    val players = PlayerStorage()

    init {
        logger.debug { "World $id is starting. eventMessage: \"$eventMessage\", Rate: ($expRate,$dropRate,$mesoRate)" }
        runningMessengerId.set(1)
        runningPartyId.set(1)
    }

    fun getChannel(ch: Int) = channels[ch - 1]

    fun addChannel(channel: Channel) = channels.add(channel)

    fun removeChannel(ch: Int) = channels.removeAt(ch)

    fun removePlayer(chr: Character) {
        channels[chr.client.channel - 1].removePlayer(chr)
        players.removePlayer(chr.id)
    }

    fun getGuild(mgc: GuildCharacter?): Guild? {
        if (mgc == null) return null
        val gId = mgc.guildId
        val g = Server.getGuild(gId, mgc) ?: return null
        if (!gsStore.containsKey(gId)) gsStore[gId] = GuildSummary(g)
        return g
    }

    fun getGuildSummary(gId: Int): GuildSummary? {
        gsStore[gId]?.let { return it }
        val g = Server.getGuild(gId)
        if (g != null) {
            gsStore[gId] = GuildSummary(g)
        }
        return gsStore[gId]
    }

    private fun updateGuildSummary(gId: Int, mgs: GuildSummary) = gsStore.put(gId, mgs)

    fun setOfflineGuildStatus(guildId: Int, guildRank: Int, cid: Int) {
        try {
            transaction {
                Characters.update({ Characters.id eq cid }) {
                    it[Characters.guildId] = guildId
                    it[Characters.guildRank] = guildRank
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Error caused when set offline guild status." }
        }
    }

    fun setGuildAndRank(cid: List<Int>, guildId: Int, rank: Int, exception: Int) {
        cid.forEach { if (it != exception) setGuildAndRank(it, guildId, rank) }
    }

    fun setGuildAndRank(cid: Int, guildId: Int, rank: Int) {
        val mc = players.getCharacterById(cid) ?: return
        val differentGuild = if (guildId == -1 && rank == -1) {
            true
        } else {
            mc.guildId = guildId
            mc.guildRank = rank
            mc.saveGuildStatus()
            guildId != mc.guildId
        }
        if (differentGuild) {
            mc.map.broadcastMessage(mc, GameplayPacket.removePlayerFromMap(cid), false)
            mc.map.broadcastMessage(mc, GameplayPacket.spawnPlayerMapObject(mc), false)
        }
    }

    fun changeGuildEmblem(gId: Int, affectedPlayers: List<Int>, mgs: GuildSummary) {
        updateGuildSummary(gId, mgs)
        sendPacket(affectedPlayers, GuildPacket.guildEmblemChange(gId, mgs.logoBG, mgs.logoBGColor, mgs.logo, mgs.logoColor), -1)
    }

    fun sendPacket(targetIds: List<Int>, packet: ByteArray, exception: Int) {
        targetIds.forEach { c ->
            if (c == exception) return@forEach
            players.getCharacterById(c)?.client?.announce(packet)
        }
    }

    fun createParty(creator: PartyCharacter): Party {
        val partyId = runningPartyId.getAndIncrement()
        val party = Party(partyId, creator)
        parties[party.id] = party
        return party
    }

    fun getParty(partyId: Int) = parties[partyId]

    private fun disbandParty(partyId: Int) = parties.remove(partyId)

    private fun updateParty(party: Party, operation: PartyOperation, target: PartyCharacter) {
        party.members.forEach {
            val chr = it.name?.let { it1 -> players.getCharacterByName(it1) }
            if (chr != null) {
                if (operation == PartyOperation.DISBAND) {
                    chr.party = null
                    chr.mpc = null
                } else {
                    chr.party = party
                    chr.mpc = it
                }
                chr.client.announce(InteractPacket.updateParty(chr.client.channel, party, operation, target))
            }
        }
        when (operation) {
            PartyOperation.LEAVE, PartyOperation.EXPEL -> {
                val chr = target.name?.let { players.getCharacterByName(it) }
                chr?.client?.announce(InteractPacket.updateParty(chr.client.channel, party, operation, target))
                chr?.party = null
                chr?.mpc = null
            }
            else -> return
        }
    }

    fun updateParty(partyId: Int, operation: PartyOperation, target: PartyCharacter) {
        val party = parties[partyId] ?: return
        target.let {
            when (operation) {
                PartyOperation.JOIN -> party.addMember(it)
                PartyOperation.EXPEL, PartyOperation.LEAVE -> party.removeMember(it)
                PartyOperation.DISBAND -> disbandParty(partyId)
                PartyOperation.SILENT_UPDATE, PartyOperation.LOG_ONOFF -> party.updateMember(it)
                PartyOperation.CHANGE_LEADER -> party.leader = it
            }
        }
        updateParty(party, operation, target)
    }

    fun findChannelIdByCharacterName(name: String): Int {
        return players.getCharacterByName(name)?.client?.channel ?: -1
    }

    fun findChannelIdByCharacterId(id: Int): Int {
        return players.getCharacterById(id)?.client?.channel ?: -1
    }

    fun partyChat(party: Party, text: String, nameFrom: String) {
        party.members.forEach {
            if (it.name != nameFrom) {
                val chr = it.name?.let { it1 -> players.getCharacterByName(it1) }
                chr?.client?.announce(InteractPacket.multiChat(nameFrom, text, 1))
            }
        }
    }

    fun buddyChat(recipientCharacterIds: Array<Int>, cidFrom: Int, nameFrom: String, text: String) {
        recipientCharacterIds.forEach { id ->
            val chr = players.getCharacterById(id)
            if (chr?.buddyList?.containsVisible(cidFrom) == true) {
                chr.client.announce(InteractPacket.multiChat(nameFrom, text, 0))
            }
        }
    }

    fun multiBuddyFind(charIdFrom: Int, characterIds: List<Int>): List<CharacterIdChannelPair> {
        val found = mutableListOf<CharacterIdChannelPair>()
        channels.forEach { ch ->
            ch.multiBuddyFind(charIdFrom, characterIds).forEach {
                found.add(CharacterIdChannelPair(it, ch.channelId))
            }
        }
        return found
    }

    fun getMessenger(mId: Int) = messengers[mId]

    fun leaveMessenger(mId: Int, target: MessengerCharacter) {
        val m = messengers[mId] ?: return
        val pos = m.getPositionByName(target.name)
        m.removeMember(target, pos)
        removeMessengerPlayer(m, pos)
    }

    fun messengerInvite(senderName: String, mId: Int, targetName: String, fromChannel: Int) {
        if (isConnected(targetName)) {
            val m = players.getCharacterByName(targetName)?.messenger
            if (m == null) {
                players.getCharacterByName(targetName)?.client?.announce(InteractPacket.messengerInvite(senderName, mId))
                val from = channels[fromChannel].players.getCharacterByName(senderName)
                from?.client?.announce(InteractPacket.messengerNote(targetName, 4, 1))
            } else {
                val from = channels[fromChannel].players.getCharacterByName(senderName)
                from?.client?.announce(InteractPacket.messengerChat("$senderName : $targetName is already using Messenger"))
            }
        }
    }

    private fun addMessengerPlayer(m: Messenger, nameFrom: String, fromChannel: Int, position: Int) {
        m.members.forEach {
            if (it.name != nameFrom) {
                val chr = players.getCharacterByName(it.name)
                val from = channels[fromChannel].players.getCharacterByName(nameFrom)
                chr?.client?.announce(InteractPacket.addMessengerPlayer(nameFrom, from, position, fromChannel - 1, true))
                from?.client?.announce(InteractPacket.addMessengerPlayer(chr?.name, chr, it.position, it.channel - 1, false))

            } else {
                val chr = players.getCharacterByName(it.name)
                chr?.client?.announce(InteractPacket.joinMessenger(it.position))
            }
        }
    }

    private fun removeMessengerPlayer(m: Messenger, position: Int) {
        m.members.forEach {
            val chr = players.getCharacterByName(it.name)
            chr?.client?.announce(InteractPacket.removeMessengerPlayer(position))
        }
    }

    fun messengerChat(m: Messenger, text: String, nameFrom: String) {
        m.members.forEach {
            if (it.name != nameFrom) {
                val chr = players.getCharacterByName(it.name)
                chr?.client?.announce(InteractPacket.messengerChat(text))
            }
        }
    }

    fun declineChat(targetName: String, nameFrom: String) {
        if (isConnected(targetName)) {
            val chr = players.getCharacterByName(targetName)
            chr?.client?.announce(InteractPacket.messengerNote(nameFrom, 5, 0))
        }
    }

    fun updateMessenger(mId: Int, nameFrom: String, fromChannel: Int) {
        messengers[mId]?.let { updateMessenger(it, nameFrom, fromChannel) }
    }

    fun updateMessenger(m: Messenger, nameFrom: String, fromChannel: Int) {
        val position = m.getPositionByName(nameFrom)
        m.members.forEach {
            channels[fromChannel]
            if (it.name != nameFrom) {
                val chr = players.getCharacterByName(it.name)
                chr?.client?.announce(InteractPacket.updateMessengerPlayer(nameFrom, channels[fromChannel].players.getCharacterByName(nameFrom), position, fromChannel - 1))
            }
        }
    }

    fun joinMessenger(mId: Int, target: MessengerCharacter, fromName: String, fromChannel: Int) {
        val m = messengers[mId] ?: return
        m.addMember(target)
        addMessengerPlayer(m, fromName, fromChannel, target.position)
    }

    fun createMessenger(creator: MessengerCharacter): Messenger {
        val mId = runningMessengerId.getAndIncrement()
        val m = Messenger(mId, creator)
        messengers[m.id] = m
        return m
    }

    fun isConnected(chrName: String) = players.getCharacterByName(chrName) != null

    fun whisper(senderName: String, targetName: String, channel: Int, message: String) {
        if (isConnected(targetName)) {
            players.getCharacterByName(targetName)?.client?.announce(InteractPacket.getWhisper(senderName, channel, message))
        }
    }

    fun requestBuddyAdd(addName: String, channelFrom: Int, cidFrom: Int, nameFrom: String): BuddyList.BuddyAddResult {
        val addChar = players.getCharacterByName(addName)
        if (addChar != null) {
            val buddyList = addChar.buddyList
            if (buddyList.isFull()) return BuddyList.BuddyAddResult.BUDDYLIST_FULL
            if (!buddyList.contains(cidFrom)) {
                buddyList.addBuddyRequest(addChar.client, cidFrom, nameFrom, channelFrom)
            } else if (buddyList.containsVisible(cidFrom)) {
                return BuddyList.BuddyAddResult.ALREADY_ON_LIST
            }
        }
        return BuddyList.BuddyAddResult.OK
    }

    fun buddyChanged(cid: Int, cidFrom: Int, name: String, channel: Int, operation: BuddyList.BuddyOperation) {
        val addChar = players.getCharacterById(cid)
        if (addChar != null) {
            val buddyList = addChar.buddyList
            when (operation) {
                BuddyList.BuddyOperation.ADDED -> {
                    if (buddyList.contains(cidFrom)) {
                        buddyList.addEntry(BuddyListEntry(name, "그룹 미지정", cidFrom, channel, true))
                        addChar.client.announce(InteractPacket.updateBuddyChannel(cidFrom, channel - 1))
                    }
                }
                BuddyList.BuddyOperation.DELETED -> {
                    if (buddyList.contains(cidFrom)) {
                        buddyList.buddies[cidFrom]
                            ?.let { BuddyListEntry(name, "그룹 미지정", cidFrom, -1, it.visible) }
                            ?.let { buddyList.addEntry(it) }
                        addChar.client.announce(InteractPacket.updateBuddyChannel(cidFrom, -1))
                    }
                }
            }
        }
    }

    fun buddyLoggedOff(characterId: Int, channel: Int, buddies: BuddyList) = updateBuddies(characterId, channel, buddies, true)

    fun buddyLoggedOn(characterId: Int, channel: Int, buddies: BuddyList) = updateBuddies(characterId, channel, buddies, false)

    private fun updateBuddies(characterId: Int, channel: Int, buddies: BuddyList, offline: Boolean) {
        buddies.buddies.forEach { (characterId, entry) ->
            val chr = players.getCharacterById(entry.characterId) ?: return@forEach
            val ble = chr.buddyList.buddies[characterId] ?: return@forEach
            if (ble.visible && entry.visible) {
                val mcChannel = if (offline) {
                    ble.channel = -1
                    -1
                } else {
                    ble.channel = channel
                    channel - 1
                }
                chr.buddyList.addEntry(ble)
                chr.client.announce(InteractPacket.updateBuddyChannel(ble.channel, mcChannel))
            }
        }
    }

    fun setServerMessage(message: String) = channels.forEach { it.serverMessage = message }

    fun broadcastPacket(data: ByteArray) = players.getAllCharacters().forEach { it.announce(data) }

    fun shutdown() {
        channels.forEach { it.shutdown() }
        players.disconnectAll()
    }

    fun allSave() = players.getAllCharacters().forEach { it.saveToDatabase() }

    suspend fun reload(): Boolean {
        with (ServerJSON.settings.worlds[id]) {
            setServerMessage(this.serverMessage)
            this@World.eventMessage = eventMessage
            this@World.flag = flag
            this@World.dropRate = this.rates.drop
            this@World.mesoRate = this.rates.meso
            this@World.expRate = this.rates.exp
        }
        return true
    }

    companion object : KLogging()
}