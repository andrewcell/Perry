package tools.packet

import client.BuddyListEntry
import client.Character
import client.Client
import client.inventory.Item
import net.SendPacketOpcode
import net.server.channel.handlers.PlayerInteractionHandler
import net.server.world.Party
import net.server.world.PartyCharacter
import net.server.world.PartyOperation
import server.PlayerShop
import server.PlayerShopItem
import server.Trade
import tools.data.output.LittleEndianWriter
import tools.data.output.PacketLittleEndianWriter
import java.awt.Point

//Packet for action with other players.
class InteractPacket {
    companion object {
        fun addMessengerPlayer(
            from: String?,
            chr: Character?,
            position: Int,
            channel: Int,
            invite: Boolean
        ): ByteArray {
            if (chr == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MESSENGER.value)
            lew.byte(0x00)
            lew.byte(position)
            CharacterPacket.addCharLook(chr, true)
            lew.gameASCIIString(from.toString())
            lew.byte(channel)
            lew.bool(invite)
            return lew.getPacket()
        }

        private fun addPartyStatus(forChannel: Int, party: Party, lew: LittleEndianWriter, leaving: Boolean) {
            val partyMembers = party.members

            while (partyMembers.size < 6) {
                partyMembers.add(PartyCharacter())
            }
            for (partyChar in partyMembers) {
                lew.int(partyChar.id ?: -1)
            }
            for (partyChar in partyMembers) {
                lew.ASCIIString(partyChar.name.toString(), 13)
            }
            for (partyChar in partyMembers) {
                lew.int(partyChar.jobId ?: 0)
            }
            for (partyChar in partyMembers) {
                lew.int((partyChar.level ?: 1).toInt())
            }
            for (partyChar in partyMembers) {
                if (partyChar.online) {
                    lew.int((partyChar.channel ?: 0) - 1)
                } else {
                    lew.int(-2)
                }
            }
            //lew.int(party.getLeader().getId());
            for (partyChar in partyMembers) {
                if (partyChar.channel == forChannel) {
                    lew.int(partyChar.mapId ?: 0)
                } else {
                    lew.int(0)
                }
            }
            for (partyChar in partyMembers) {
                if (partyChar.channel == forChannel && !leaving) {
                    lew.int(partyChar.doorTown)
                    lew.int(partyChar.doorTarget)
                    lew.int(partyChar.doorPosition.x)
                    lew.int(partyChar.doorPosition.y)
                } else {
                    lew.int(999999999)
                    lew.int(999999999)
                    lew.int(0)
                    lew.int(0)
                }
            }
        }

        /**
         * @param c
         * @param shop
         * @param owner
         * @return
         */
        fun getPlayerShop(c: Client, shop: PlayerShop, owner: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.ROOM.code)
            lew.byte(4)
            lew.byte(4)
            lew.byte(if (owner) 0 else 1)
            lew.byte(0)
            CharacterPacket.addCharLook(shop.owner, false)
            lew.gameASCIIString(shop.owner.name)
            lew.byte(1)
            CharacterPacket.addCharLook(shop.owner, false)
            lew.gameASCIIString(shop.owner.name)
            lew.byte(0xFF)
            lew.gameASCIIString(shop.description)
            val items: List<PlayerShopItem> = shop.items
            lew.byte(0x10)
            lew.byte(items.size)
            for ((item1, bundles, price) in items) {
                lew.short(bundles.toInt())
                lew.short(item1.quantity.toInt())
                lew.int(price)
                ItemPacket.addItemInfo(item1, true, true)
            }
            return lew.getPacket()
        }

        fun getPlayerShopChat(c: Character?, chat: String, slot: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.CHAT.code)
            lew.byte(PlayerInteractionHandler.Action.CHAT_THING.code)
            lew.byte(slot)
            lew.gameASCIIString("${c?.name} : $chat")
            return lew.getPacket()
        }

        fun getPlayerShopChat(c: Character?, chat: String, owner: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.CHAT.code)
            lew.byte(PlayerInteractionHandler.Action.CHAT_THING.code)
            lew.byte(if (owner) 0 else 1)
            lew.gameASCIIString("${c?.name} : $chat")
            return lew.getPacket()
        }

        fun getPlayerShopItemUpdate(shop: PlayerShop): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.UPDATE_MERCHANT.code)
            lew.byte(shop.items.size)
            for ((item1, bundles, price) in shop.items) {
                lew.short(bundles.toInt())
                lew.short(item1.quantity.toInt())
                lew.int(price)
                ItemPacket.addItemInfo(item1, true, true)
            }
            return lew.getPacket()
        }

        fun getPlayerShopRemoveVisitor(slot: Int): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.EXIT.code)
            if (slot > 0) {
                lew.byte(slot)
            }
            return lew.getPacket()
        }

        /**
         * Gets a general chat packet.
         *
         * @param cidfrom The character ID who sent the chat.
         * @param text    The text of the chat.
         * @param show
         * @return The general chat packet.
         */
        fun getChatText(cidfrom: Int, text: String, gm: Boolean, show: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CHATTEXT.value)
            lew.int(cidfrom)
            lew.bool(gm)
            lew.gameASCIIString(text)
            lew.short(show)
            return lew.getPacket()
        }

        /**
         * Gets a packet telling the client to show a fame gain.
         *
         * @param gain How many fame gained.
         * @return The meso gain packet.
         */
        fun getShowFameGain(gain: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.byte(4)
            lew.int(gain)
            return lew.getPacket()
        }

        fun getTradeCancel(number: Byte): ByteArray {
            val lew = PacketLittleEndianWriter(5)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.EXIT.code)
            lew.byte(number)
            lew.byte(2)
            return lew.getPacket()
        }

        fun getTradeChat(c: Character, chat: String, owner: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.CHAT.code)
            lew.byte(PlayerInteractionHandler.Action.CHAT_THING.code)
            lew.byte(if (owner) 0 else 1)
            lew.gameASCIIString(c.name + " : " + chat)
            return lew.getPacket()
        }

        fun getTradeConfirmation(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.CONFIRM.code)
            return lew.getPacket()
        }

        fun getTradeCompletion(number: Byte): ByteArray {
            val lew = PacketLittleEndianWriter(5)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.EXIT.code)
            lew.byte(number)
            lew.byte(6)
            return lew.getPacket()
        }

        fun getTradeInvite(c: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.INVITE.code)
            lew.byte(3)
            lew.gameASCIIString(c.name)
            lew.byte(byteArrayOf(0xB7.toByte(), 0x50.toByte(), 0, 0))
            return lew.getPacket()
        }

        fun getTradeItemAdd(number: Byte, item: Item): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.SET_ITEMS.code)
            lew.byte(number)
            //lew.byte(item.getPosition());
            ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = true)
            return lew.getPacket()
        }

        fun getTradeMesoSet(number: Byte, meso: Int): ByteArray {
            val lew = PacketLittleEndianWriter(8)
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.SET_MESO.code)
            lew.byte(number)
            lew.int(meso)
            return lew.getPacket()
        }

        fun getTradePartnerAdd(c: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.VISIT.code)
            lew.byte(1)
            CharacterPacket.addCharLook(c, false)
            lew.gameASCIIString(c.name)
            return lew.getPacket()
        }

        fun getTradeStart(c: Client, trade: Trade?, number: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.ROOM.code)
            lew.byte(3)
            lew.byte(2)
            lew.byte(number)
            if (number.toInt() == 1) {
                lew.byte(0)
                trade?.partner?.let { CharacterPacket.addCharLook(it.chr, false) }
                lew.gameASCIIString(trade?.partner?.chr?.name.toString())
            }
            lew.byte(number)
            c.player?.let { CharacterPacket.addCharLook(it, false) }
            lew.gameASCIIString(c.player?.name.toString())
            lew.byte(0xFF)
            return lew.getPacket()
        }

        fun getWhisper(sender: String, channel: Int, text: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.WHISPER.value)
            lew.byte(0x12)
            lew.gameASCIIString(sender)
            lew.byte(channel - 1) // I guess this is the channel
            lew.gameASCIIString(text)
            return lew.getPacket()
        }

        /**
         * @param target name of the target character
         * @param reply  error code: 0x0 = cannot find char, 0x1 = success
         * @return the Packet
         */
        fun getWhisperReply(target: String, reply: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.WHISPER.value)
            lew.byte(0x0A) // whisper?
            lew.gameASCIIString(target)
            lew.byte(reply)
            return lew.getPacket()
        }

        /**
         * @param target
         * @param mapid
         * @param MTSMapCSChannel 0: MTS 1: Map 2: CS 3: Different Channel
         * @return
         */
        fun getWhisperFindReply(target: String?, mapid: Int, MTSMapCSChannel: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.WHISPER.value)
            lew.byte(9)
            lew.gameASCIIString(target!!)
            lew.byte(MTSMapCSChannel) // 0: mts 1: map 2: cs
            lew.int(mapid) // -1 if mts, cs
            if (MTSMapCSChannel == 1) {
                lew.byte(ByteArray(8))
            }
            return lew.getPacket()
        }

        /**
         * status can be: <br></br> 0: ok, use giveFameResponse<br></br> 1: the username is
         * incorrectly entered<br></br> 2: users under level 15 are unable to toggle with
         * fame.<br></br> 3: can't raise or drop fame anymore today.<br></br> 4: can't raise
         * or drop fame for this character for this month anymore.<br></br> 5: received
         * fame, use receiveFame()<br></br> 6: level of fame neither has been raised nor
         * dropped due to an unexpected error
         *
         * @param status
         * @return
         */
        fun giveFameErrorResponse(status: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.FAME_RESPONSE.value)
            lew.byte(status)
            return lew.getPacket()
        }

        fun giveFameResponse(mode: Int, charName: String, newFame: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.FAME_RESPONSE.value)
            lew.byte(0)
            lew.gameASCIIString(charName)
            lew.byte(mode)
            lew.short(newFame)
            lew.short(0)
            return lew.getPacket()
        }

        fun joinMessenger(position: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MESSENGER.value)
            lew.byte(0x01)
            lew.byte(position)
            return lew.getPacket()
        }

        fun messengerChat(text: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MESSENGER.value)
            lew.byte(0x06)
            lew.gameASCIIString(text)
            return lew.getPacket()
        }

        fun messengerInvite(from: String, messengerId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MESSENGER.value)
            lew.byte(0x03)
            lew.gameASCIIString(from)
            lew.byte(0)
            lew.int(messengerId)
            lew.byte(0)
            return lew.getPacket()
        }

        fun messengerNote(text: String, mode: Int, mode2: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MESSENGER.value)
            lew.byte(mode)
            lew.gameASCIIString(text)
            lew.byte(mode2)
            return lew.getPacket()
        }

        /**
         * mode: 0 buddychat; 1 partychat; 2 guildchat
         *
         * @param name
         * @param chattext
         * @param mode
         * @return
         */
        fun multiChat(name: String, chatText: String, mode: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MULTICHAT.value)
            lew.byte(mode)
            lew.gameASCIIString(name)
            lew.gameASCIIString(chatText)
            return lew.getPacket()
        }

        fun partyCreated(partyId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PARTY_OPERATION.value)
            lew.byte(7)
            lew.int(partyId)
            lew.int(999999999)
            lew.int(999999999)
            lew.int(0)
            return lew.getPacket()
        }

        fun partyInvite(from: Character?): ByteArray {
            val writer = PacketLittleEndianWriter()
            writer.byte(SendPacketOpcode.PARTY_OPERATION.value)
            writer.byte(4)
            writer.int((from?.party?.id ?: 0))
            writer.gameASCIIString(from?.name ?: "")
            return writer.getPacket()
        }

        fun partyPortal(townId: Int, targetId: Int, position: Point): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PARTY_OPERATION.value)
            lew.short(0x23)
            lew.int(townId)
            lew.int(targetId)
            lew.pos(position)
            return lew.getPacket()
        }

        /**
         * 10: A beginner can't create a party. 1/11/14/19: Your request for a party
         * didn't work due to an unexpected error. 13: You have yet to join a party.
         * 16: Already have joined a party. 17: The party you're trying to join is
         * already in full capacity. 19: Unable to find the requested character in
         * this channel.
         *
         * @param message
         * @return
         */
        fun partyStatusMessage(message: Int): ByteArray {
            val pew = PacketLittleEndianWriter()
            pew.short(SendPacketOpcode.PARTY_OPERATION.value)
            pew.byte(message)
            return pew.getPacket()
        }

        /**
         * 23: 'Char' have denied request to the party.
         *
         * @param message
         * @param charname
         * @return
         */
        fun partyStatusMessage(message: Int, charName: String?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PARTY_OPERATION.value)
            lew.byte(message)
            lew.gameASCIIString(charName.toString())
            return lew.getPacket()
        }

        fun removeMessengerPlayer(position: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MESSENGER.value)
            lew.byte(0x02)
            lew.byte(position)
            return lew.getPacket()
        }

        fun receiveFame(mode: Int, charNameFrom: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.FAME_RESPONSE.value)
            lew.byte(5)
            lew.gameASCIIString(charNameFrom)
            lew.byte(mode)
            return lew.getPacket()
        }

        fun requestBuddyListAdd(cidFrom: Int, cid: Int, nameFrom: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.BUDDYLIST.value)
            lew.byte(9)
            lew.int(cidFrom)
            lew.gameASCIIString(nameFrom)
            lew.int(cidFrom)
            lew.ASCIIString(nameFrom, 11)
            lew.byte(0x09)
            lew.byte(0xf0)
            lew.byte(0x01)
            lew.int(0x0f)
            lew.int(cid)
            return lew.getPacket()
        }

        /**
         * Gets a server notice packet.
         *
         * Possible values for
         * `type`:<br></br> 0: [Notice]<br></br> 1: Popup<br></br> 2: Megaphone<br></br> 3:
         * Super Megaphone<br></br> 4: Scrolling message at top<br></br> 5: Pink Text<br></br> 6:
         * Lightblue Text
         *
         * @param type The type of the notice.
         * @param message The message to convey.
         * @return The server notice packet.
         */
        fun serverNotice(type: Int, message: String): ByteArray {
            return serverMessage(type, 0, message, serverMessage = false, megaEar = false)
        }

        fun serverNotice(type: Int, channel: Int, message: String, sMegaEar: Boolean = false): ByteArray {
            return serverMessage(type, channel, message, false, sMegaEar)
        }

        /**
         * Gets a server message packet.
         *
         * Possible values for
         * `type`:<br></br> 0: [Notice]<br></br> 1: Popup<br></br> 2: Megaphone<br></br> 3:
         * Super Megaphone<br></br> 4: Scrolling message at top<br></br> 5: Pink Text<br></br> 6:
         * Lightblue Text
         *
         * @param type The type of the notice.
         * @param channel The channel this notice was sent on.
         * @param message The message to convey.
         * @param serverMessage Is this a scrolling ticker?
         * @return The server notice packet.
         */
        fun serverMessage(
            type: Int = 4,
            channel: Int = 0,
            message: String,
            serverMessage: Boolean = true,
            megaEar: Boolean = false
        ): ByteArray {
            val writer = PacketLittleEndianWriter()
            writer.byte(SendPacketOpcode.SERVERMESSAGE.value)
            writer.byte(type)
            if (serverMessage) {
                writer.byte(1)
            }
            writer.gameASCIIString(message)
            if (type == 3) {
                writer.byte(channel - 1) // channel
                writer.bool(megaEar)
            } else if (type == 6) {
                writer.int(0)
            }
            return writer.getPacket()
        }

        fun shopErrorMessage(error: Int, type: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(0x0A)
            lew.byte(type)
            lew.byte(error)
            return lew.getPacket()
        }

        fun itemMegaphone(msg: String, whisper: Boolean, channel: Int, item: Item?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SERVERMESSAGE.value)
            lew.byte(8)
            lew.gameASCIIString(msg)
            lew.byte(channel - 1)
            lew.byte(if (whisper) 1 else 0)
            if (item == null) {
                lew.byte(0)
            } else {
                lew.byte(item.position)
                ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = true)
            }
            return lew.getPacket()
        }

        /**
         * Sends the Gachapon green message when a user uses a gachapon ticket.
         *
         * @param item
         * @param town
         * @param player
         * @return
         */
        fun gachaponMessage(item: Item?, town: String, player: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SERVERMESSAGE.value)
            lew.byte(0x0B)
            lew.gameASCIIString("${player?.name} : got a(n)")
            lew.int(0) //random?
            lew.gameASCIIString(town)
            item?.let { ItemPacket.addItemInfo(it, zeroPosition = true, leaveOut = true) }
            return lew.getPacket()
        }

        /**
         * Sends a Avatar Super Megaphone packet.
         *
         * @param chr     The character name.
         * @param medal   The medal text.
         * @param channel Which channel.
         * @param itemId  Which item used.
         * @param message The message sent.
         * @param ear     Whether or not the ear is shown for whisper.
         * @return
         */
        fun getAvatarMega(
            chr: Character,
            medal: String,
            channel: Int,
            itemId: Int,
            message: List<String>,
            ear: Boolean
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SET_AVATAR_MEGAPHONE.value)
            lew.int(itemId)
            lew.gameASCIIString(medal + chr.name)
            for (s in message) {
                lew.gameASCIIString(s)
            }
            lew.int(channel - 1) // channel
            lew.bool(ear)
            CharacterPacket.addCharLook(chr, true)
            return lew.getPacket()
        }

        fun updateBuddyList(buddyList: Collection<BuddyListEntry>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.BUDDYLIST.value)
            lew.byte(7)
            var size = 0
            for ((_, _, _, _, visible) in buddyList) {
                if (visible) {
                    size++
                }
            }
            lew.byte(size)
            for ((name1, _, characterId, channel, visible) in buddyList) {
                if (visible) {
                    lew.int(characterId) // cid
                    lew.ASCIIString(name1, 13)
                    lew.byte(0) // opposite status
                    lew.int(channel - 1)
                }
            }
            for (x in buddyList.indices) {
                lew.int(0) //mapid?
            }
            return lew.getPacket()
        }


        fun updateParty(forChannel: Int, party: Party?, op: PartyOperation, target: PartyCharacter?): ByteArray {
            if (party == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PARTY_OPERATION.value)
            when (op) {
                PartyOperation.DISBAND, PartyOperation.EXPEL, PartyOperation.LEAVE -> {
                    lew.byte(0x0B)
                    lew.int(40546)
                    lew.int(target?.id ?: -1)
                    if (op === PartyOperation.DISBAND) {
                        lew.byte(0)
                        //lew.int(party.getId());
                    } else {
                        lew.byte(1)
                        if (op === PartyOperation.EXPEL) {
                            lew.byte(1)
                        } else {
                            lew.byte(0)
                        }
                        lew.gameASCIIString(target?.name.toString())
                        addPartyStatus(forChannel, party, lew, false)
                    }
                }
                PartyOperation.JOIN -> {
                    lew.byte(0xE)
                    lew.int(40546)
                    lew.gameASCIIString(target?.name ?: "")
                    addPartyStatus(forChannel, party, lew, false)
                }
                PartyOperation.SILENT_UPDATE, PartyOperation.LOG_ONOFF -> {
                    lew.byte(0x6)
                    lew.int(party.id)
                    addPartyStatus(forChannel, party, lew, false)
                }
                else -> {}
            }
            return lew.getPacket()
        }

        fun updateBuddyChannel(characterId: Int, channel: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.BUDDYLIST.value)
            lew.byte(0x14)
            lew.int(characterId)
            lew.byte(0)
            lew.int(channel)
            return lew.getPacket()
        }

        fun updateMessengerPlayer(from: String, chr: Character?, position: Int, channel: Int): ByteArray {
            if (chr == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MESSENGER.value)
            lew.byte(0x07)
            lew.byte(position)
            CharacterPacket.addCharLook( chr, true)
            lew.gameASCIIString(from)
            lew.byte(channel)
            lew.byte(0x00)
            return lew.getPacket()
        }

        fun updatePartyMemberHp(cid: Int, curHp: Int, maxHp: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.value)
            lew.int(cid)
            lew.int(curHp)
            lew.int(maxHp)
            return lew.getPacket()
        }
    }
}