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
            lew.write(SendPacketOpcode.MESSENGER.value)
            lew.write(0x00)
            lew.write(position)
            CharacterPacket.addCharLook(lew, chr, true)
            lew.writeGameASCIIString(from.toString())
            lew.write(channel)
            lew.writeBool(invite)
            return lew.getPacket()
        }

        private fun addPartyStatus(forChannel: Int, party: Party, lew: LittleEndianWriter, leaving: Boolean) {
            val partyMembers = party.members

            while (partyMembers.size < 6) {
                partyMembers.add(PartyCharacter())
            }
            for (partyChar in partyMembers) {
                lew.writeInt(partyChar.id ?: -1)
            }
            for (partyChar in partyMembers) {
                lew.writeASCIIString(partyChar.name.toString(), 13)
            }
            for (partyChar in partyMembers) {
                lew.writeInt(partyChar.jobId ?: 0)
            }
            for (partyChar in partyMembers) {
                lew.writeInt((partyChar.level ?: 1).toInt())
            }
            for (partyChar in partyMembers) {
                if (partyChar.online) {
                    lew.writeInt((partyChar.channel ?: 0) - 1)
                } else {
                    lew.writeInt(-2)
                }
            }
            //lew.writeInt(party.getLeader().getId());
            for (partyChar in partyMembers) {
                if (partyChar.channel == forChannel) {
                    lew.writeInt(partyChar.mapId ?: 0)
                } else {
                    lew.writeInt(0)
                }
            }
            for (partyChar in partyMembers) {
                if (partyChar.channel == forChannel && !leaving) {
                    lew.writeInt(partyChar.doorTown)
                    lew.writeInt(partyChar.doorTarget)
                    lew.writeInt(partyChar.doorPosition.x)
                    lew.writeInt(partyChar.doorPosition.y)
                } else {
                    lew.writeInt(999999999)
                    lew.writeInt(999999999)
                    lew.writeInt(0)
                    lew.writeInt(0)
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
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.ROOM.code)
            lew.write(4)
            lew.write(4)
            lew.write(if (owner) 0 else 1)
            lew.write(0)
            CharacterPacket.addCharLook(lew, shop.owner, false)
            lew.writeGameASCIIString(shop.owner.name)
            lew.write(1)
            CharacterPacket.addCharLook(lew, shop.owner, false)
            lew.writeGameASCIIString(shop.owner.name)
            lew.write(0xFF)
            lew.writeGameASCIIString(shop.description)
            val items: List<PlayerShopItem> = shop.items
            lew.write(0x10)
            lew.write(items.size)
            for ((item1, bundles, price) in items) {
                lew.writeShort(bundles.toInt())
                lew.writeShort(item1.quantity.toInt())
                lew.writeInt(price)
                ItemPacket.addItemInfo(lew, item1, true, true)
            }
            return lew.getPacket()
        }

        fun getPlayerShopChat(c: Character?, chat: String, slot: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.CHAT.code)
            lew.write(PlayerInteractionHandler.Action.CHAT_THING.code)
            lew.write(slot)
            lew.writeGameASCIIString("${c?.name} : $chat")
            return lew.getPacket()
        }

        fun getPlayerShopChat(c: Character?, chat: String, owner: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.CHAT.code)
            lew.write(PlayerInteractionHandler.Action.CHAT_THING.code)
            lew.write(if (owner) 0 else 1)
            lew.writeGameASCIIString("${c?.name} : $chat")
            return lew.getPacket()
        }

        fun getPlayerShopItemUpdate(shop: PlayerShop): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.UPDATE_MERCHANT.code)
            lew.write(shop.items.size)
            for ((item1, bundles, price) in shop.items) {
                lew.writeShort(bundles.toInt())
                lew.writeShort(item1.quantity.toInt())
                lew.writeInt(price)
                ItemPacket.addItemInfo(lew, item1, true, true)
            }
            return lew.getPacket()
        }

        fun getPlayerShopRemoveVisitor(slot: Int): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.EXIT.code)
            if (slot > 0) {
                lew.write(slot)
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
            lew.write(SendPacketOpcode.CHATTEXT.value)
            lew.writeInt(cidfrom)
            lew.writeBool(gm)
            lew.writeGameASCIIString(text)
            lew.writeShort(show)
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
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(4)
            lew.writeInt(gain)
            return lew.getPacket()
        }

        fun getTradeCancel(number: Byte): ByteArray {
            val lew = PacketLittleEndianWriter(5)
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.EXIT.code)
            lew.write(number)
            lew.write(2)
            return lew.getPacket()
        }

        fun getTradeChat(c: Character, chat: String, owner: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.CHAT.code)
            lew.write(PlayerInteractionHandler.Action.CHAT_THING.code)
            lew.write(if (owner) 0 else 1)
            lew.writeGameASCIIString(c.name + " : " + chat)
            return lew.getPacket()
        }

        fun getTradeConfirmation(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.CONFIRM.code)
            return lew.getPacket()
        }

        fun getTradeCompletion(number: Byte): ByteArray {
            val lew = PacketLittleEndianWriter(5)
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.EXIT.code)
            lew.write(number)
            lew.write(6)
            return lew.getPacket()
        }

        fun getTradeInvite(c: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.INVITE.code)
            lew.write(3)
            lew.writeGameASCIIString(c.name)
            lew.write(byteArrayOf(0xB7.toByte(), 0x50.toByte(), 0, 0))
            return lew.getPacket()
        }

        fun getTradeItemAdd(number: Byte, item: Item): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.SET_ITEMS.code)
            lew.write(number)
            //lew.write(item.getPosition());
            ItemPacket.addItemInfo(lew, item, zeroPosition = false, leaveOut = false, trade = true)
            return lew.getPacket()
        }

        fun getTradeMesoSet(number: Byte, meso: Int): ByteArray {
            val lew = PacketLittleEndianWriter(8)
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.SET_MESO.code)
            lew.write(number)
            lew.writeInt(meso)
            return lew.getPacket()
        }

        fun getTradePartnerAdd(c: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.VISIT.code)
            lew.write(1)
            CharacterPacket.addCharLook(lew, c, false)
            lew.writeGameASCIIString(c.name)
            return lew.getPacket()
        }

        fun getTradeStart(c: Client, trade: Trade?, number: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.ROOM.code)
            lew.write(3)
            lew.write(2)
            lew.write(number)
            if (number.toInt() == 1) {
                lew.write(0)
                trade?.partner?.let { CharacterPacket.addCharLook(lew, it.chr, false) }
                lew.writeGameASCIIString(trade?.partner?.chr?.name.toString())
            }
            lew.write(number)
            c.player?.let { CharacterPacket.addCharLook(lew, it, false) }
            lew.writeGameASCIIString(c.player?.name.toString())
            lew.write(0xFF)
            return lew.getPacket()
        }

        fun getWhisper(sender: String, channel: Int, text: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.WHISPER.value)
            lew.write(0x12)
            lew.writeGameASCIIString(sender)
            lew.write(channel - 1) // I guess this is the channel
            lew.writeGameASCIIString(text)
            return lew.getPacket()
        }

        /**
         * @param target name of the target character
         * @param reply  error code: 0x0 = cannot find char, 0x1 = success
         * @return the Packet
         */
        fun getWhisperReply(target: String, reply: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.WHISPER.value)
            lew.write(0x0A) // whisper?
            lew.writeGameASCIIString(target)
            lew.write(reply)
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
            lew.write(SendPacketOpcode.WHISPER.value)
            lew.write(9)
            lew.writeGameASCIIString(target!!)
            lew.write(MTSMapCSChannel) // 0: mts 1: map 2: cs
            lew.writeInt(mapid) // -1 if mts, cs
            if (MTSMapCSChannel == 1) {
                lew.write(ByteArray(8))
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
            lew.write(SendPacketOpcode.FAME_RESPONSE.value)
            lew.write(status)
            return lew.getPacket()
        }

        fun giveFameResponse(mode: Int, charName: String, newFame: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.FAME_RESPONSE.value)
            lew.write(0)
            lew.writeGameASCIIString(charName)
            lew.write(mode)
            lew.writeShort(newFame)
            lew.writeShort(0)
            return lew.getPacket()
        }

        fun joinMessenger(position: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MESSENGER.value)
            lew.write(0x01)
            lew.write(position)
            return lew.getPacket()
        }

        fun messengerChat(text: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MESSENGER.value)
            lew.write(0x06)
            lew.writeGameASCIIString(text)
            return lew.getPacket()
        }

        fun messengerInvite(from: String, messengerId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MESSENGER.value)
            lew.write(0x03)
            lew.writeGameASCIIString(from)
            lew.write(0)
            lew.writeInt(messengerId)
            lew.write(0)
            return lew.getPacket()
        }

        fun messengerNote(text: String, mode: Int, mode2: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MESSENGER.value)
            lew.write(mode)
            lew.writeGameASCIIString(text)
            lew.write(mode2)
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
            lew.write(SendPacketOpcode.MULTICHAT.value)
            lew.write(mode)
            lew.writeGameASCIIString(name)
            lew.writeGameASCIIString(chatText)
            return lew.getPacket()
        }

        fun partyCreated(partyId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PARTY_OPERATION.value)
            lew.write(7)
            lew.writeInt(partyId)
            lew.writeInt(999999999)
            lew.writeInt(999999999)
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun partyInvite(from: Character?): ByteArray {
            val writer = PacketLittleEndianWriter()
            writer.write(SendPacketOpcode.PARTY_OPERATION.value)
            writer.write(4)
            writer.writeInt((from?.party?.id ?: 0))
            writer.writeGameASCIIString(from?.name ?: "")
            return writer.getPacket()
        }

        fun partyPortal(townId: Int, targetId: Int, position: Point): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PARTY_OPERATION.value)
            lew.writeShort(0x23)
            lew.writeInt(townId)
            lew.writeInt(targetId)
            lew.writePos(position)
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
            pew.writeShort(SendPacketOpcode.PARTY_OPERATION.value)
            pew.write(message)
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
            lew.write(SendPacketOpcode.PARTY_OPERATION.value)
            lew.write(message)
            lew.writeGameASCIIString(charName.toString())
            return lew.getPacket()
        }

        fun removeMessengerPlayer(position: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MESSENGER.value)
            lew.write(0x02)
            lew.write(position)
            return lew.getPacket()
        }

        fun receiveFame(mode: Int, charNameFrom: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.FAME_RESPONSE.value)
            lew.write(5)
            lew.writeGameASCIIString(charNameFrom)
            lew.write(mode)
            return lew.getPacket()
        }

        fun requestBuddyListAdd(cidFrom: Int, cid: Int, nameFrom: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.BUDDYLIST.value)
            lew.write(9)
            lew.writeInt(cidFrom)
            lew.writeGameASCIIString(nameFrom)
            lew.writeInt(cidFrom)
            lew.writeASCIIString(nameFrom, 11)
            lew.write(0x09)
            lew.write(0xf0)
            lew.write(0x01)
            lew.writeInt(0x0f)
            lew.writeInt(cid)
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
            writer.write(SendPacketOpcode.SERVERMESSAGE.value)
            writer.write(type)
            if (serverMessage) {
                writer.write(1)
            }
            writer.writeGameASCIIString(message)
            if (type == 3) {
                writer.write(channel - 1) // channel
                writer.writeBool(megaEar)
            } else if (type == 6) {
                writer.writeInt(0)
            }
            return writer.getPacket()
        }

        fun shopErrorMessage(error: Int, type: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(0x0A)
            lew.write(type)
            lew.write(error)
            return lew.getPacket()
        }

        fun itemMegaphone(msg: String, whisper: Boolean, channel: Int, item: Item?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SERVERMESSAGE.value)
            lew.write(8)
            lew.writeGameASCIIString(msg)
            lew.write(channel - 1)
            lew.write(if (whisper) 1 else 0)
            if (item == null) {
                lew.write(0)
            } else {
                lew.write(item.position)
                ItemPacket.addItemInfo(lew, item, zeroPosition = false, leaveOut = false, trade = true)
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
            lew.write(SendPacketOpcode.SERVERMESSAGE.value)
            lew.write(0x0B)
            lew.writeGameASCIIString("${player?.name} : got a(n)")
            lew.writeInt(0) //random?
            lew.writeGameASCIIString(town)
            item?.let { ItemPacket.addItemInfo(lew, it, zeroPosition = true, leaveOut = true) }
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
            lew.write(SendPacketOpcode.SET_AVATAR_MEGAPHONE.value)
            lew.writeInt(itemId)
            lew.writeGameASCIIString(medal + chr.name)
            for (s in message) {
                lew.writeGameASCIIString(s)
            }
            lew.writeInt(channel - 1) // channel
            lew.writeBool(ear)
            CharacterPacket.addCharLook(lew, chr, true)
            return lew.getPacket()
        }

        fun updateBuddyList(buddyList: Collection<BuddyListEntry>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.BUDDYLIST.value)
            lew.write(7)
            var size = 0
            for ((_, _, _, _, visible) in buddyList) {
                if (visible) {
                    size++
                }
            }
            lew.write(size)
            for ((name1, _, characterId, channel, visible) in buddyList) {
                if (visible) {
                    lew.writeInt(characterId) // cid
                    lew.writeASCIIString(name1, 13)
                    lew.write(0) // opposite status
                    lew.writeInt(channel - 1)
                }
            }
            for (x in buddyList.indices) {
                lew.writeInt(0) //mapid?
            }
            return lew.getPacket()
        }


        fun updateParty(forChannel: Int, party: Party?, op: PartyOperation, target: PartyCharacter?): ByteArray {
            if (party == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PARTY_OPERATION.value)
            when (op) {
                PartyOperation.DISBAND, PartyOperation.EXPEL, PartyOperation.LEAVE -> {
                    lew.write(0x0B)
                    lew.writeInt(40546)
                    lew.writeInt(target?.id ?: -1)
                    if (op === PartyOperation.DISBAND) {
                        lew.write(0)
                        //lew.writeInt(party.getId());
                    } else {
                        lew.write(1)
                        if (op === PartyOperation.EXPEL) {
                            lew.write(1)
                        } else {
                            lew.write(0)
                        }
                        lew.writeGameASCIIString(target?.name.toString())
                        addPartyStatus(forChannel, party, lew, false)
                    }
                }
                PartyOperation.JOIN -> {
                    lew.write(0xE)
                    lew.writeInt(40546)
                    lew.writeGameASCIIString(target?.name ?: "")
                    addPartyStatus(forChannel, party, lew, false)
                }
                PartyOperation.SILENT_UPDATE, PartyOperation.LOG_ONOFF -> {
                    lew.write(0x6)
                    lew.writeInt(party.id)
                    addPartyStatus(forChannel, party, lew, false)
                }
                else -> {}
            }
            return lew.getPacket()
        }

        fun updateBuddyChannel(characterId: Int, channel: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.BUDDYLIST.value)
            lew.write(0x14)
            lew.writeInt(characterId)
            lew.write(0)
            lew.writeInt(channel)
            return lew.getPacket()
        }

        fun updateMessengerPlayer(from: String, chr: Character?, position: Int, channel: Int): ByteArray {
            if (chr == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MESSENGER.value)
            lew.write(0x07)
            lew.write(position)
            CharacterPacket.addCharLook(lew, chr, true)
            lew.writeGameASCIIString(from)
            lew.write(channel)
            lew.write(0x00)
            return lew.getPacket()
        }

        fun updatePartyMemberHp(cid: Int, curHp: Int, maxHp: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.value)
            lew.writeInt(cid)
            lew.writeInt(curHp)
            lew.writeInt(maxHp)
            return lew.getPacket()
        }
    }
}