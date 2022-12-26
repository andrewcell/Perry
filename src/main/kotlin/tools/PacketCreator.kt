package tools

import client.*
import client.inventory.*
import client.status.MonsterStatus
import io.ktor.utils.io.*
import mu.KLogging
import net.SendPacketOpcode
import server.*
import server.maps.*
import tools.HexTool.Companion.getByteArrayFromHexString
import tools.data.output.PacketLittleEndianWriter
import tools.packet.CharacterPacket
import tools.packet.ItemPacket.Companion.addItemInfo
import java.lang.Double.doubleToLongBits
import java.sql.SQLException
import java.util.*
import kotlin.random.Random

class PacketCreator {
    companion object : KLogging() {
        val EMPTY_STATUPDATE = emptyList<Pair<CharacterStat, Int>>()
        private const val FT_UT_OFFSET = 116444592000000000L // EDT
        private const val DEFAULT_TIME = 150842304000000000L //00 80 05 BB 46 E6 17 02
        const val ZERO_TIME = 94354848000000000L //00 40 E0 FD 3B 37 4F 01
        private const val PERMANENT = 150841440000000000L // 00 C0 9B 90 7D E5 17 02

        fun packetWriter(code: SendPacketOpcode? = null, size: Int = 32, lew: PacketLittleEndianWriter.() -> Unit): ByteArray {
            return PacketLittleEndianWriter(size).apply {
                code?.let { opcode(it) }
                lew()
            }.getPacket()
        }

        fun addAnnounceBox(
            lew: PacketLittleEndianWriter,
            game: MiniGame?,
            gameType: Int,
            locker: Int?,
            type: Int,
            amMount: Int,
            joinable: Int
        ) {
            if (game == null) return
            lew.byte(gameType)
            lew.int(game.objectId) // gameid/shopid
            lew.gameASCIIString(game.description) // desc
            lew.byte(locker ?: 0)
            lew.byte(type)
            lew.byte(amMount)
            lew.byte(2)
            lew.byte(joinable)
        }

        /**
         * Adds a announcement box to an existing PacketLittleEndianWriter.
         *
         * @param lew The PacketLittleEndianWriter to add an announcement box
         * to.
         * @param shop The shop to announce.
         */
        fun addAnnounceBox(lew: PacketLittleEndianWriter, shop: PlayerShop?, availability: Int) {
            if (shop == null) return
            lew.byte(4)
            lew.int(shop.objectId)
            lew.gameASCIIString(shop.description)
            lew.byte(0)
            lew.byte(0)
            lew.byte(1)
            lew.byte(availability)
            lew.byte(0)
        }


        /**
         * Gets a "block" packet (ie. the cash shop is unavailable, etc)
         *
         *
         * Possible values for
         * `type`:<br></br> 1: The portal is closed for now.<br></br> 2: You cannot
         * go to that place.<br></br> 3: Unable to approach due to the force of the
         * ground.<br></br> 4: You cannot teleport to or on this map.<br></br> 5: Unable to
         * approach due to the force of the ground.<br></br> 6: This map can only be
         * entered by party members.<br></br> 7: The Cash Shop is currently not
         * available. Stay tuned...<br></br>
         *
         * @param type The type
         * @return The "block" packet.
         */
        fun blockedMessage(type: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.BLOCKED_MAP.value)
            lew.byte(type)
            return lew.getPacket()
        }

        fun boatPacket(type: Boolean): ByteArray { //don't think this is correct…
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CONTI_MOVE.value)
            lew.byte(if (type) 1 else 2)
            lew.byte(0)
            return lew.getPacket()
        }

        fun catchMessage(message: Int): ByteArray { // not done, I guess
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.BRIDLE_MOB_CATCH_FAIL.value)
            lew.byte(message) // 1 = too strong, 2 = Elemental Rock
            lew.int(0) //Maybe itemid?
            lew.int(0)
            return lew.getPacket()
        }

        fun customPacket(packet: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(getByteArrayFromHexString(packet))
            return lew.getPacket()
        }

        fun customPacket(packet: ByteArray): ByteArray {
            val lew = PacketLittleEndianWriter(packet.size)
            lew.byte(packet)
            return lew.getPacket()
        }


        fun disableMinimap(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.ADMIN_RESULT.value)
            lew.short(0x1C)
            return lew.getPacket()
        }

        fun disableUI(enable: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.DISABLE_UI.value)
            lew.byte(if (enable) 1 else 0)
            return lew.getPacket()
        }

        fun doubleToShortBits(d: Double): Int = (doubleToLongBits(d) shr 48).toInt()

        fun earnTitleMessage(msg: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SCRIPT_PROGRESS_MESSAGE.value)
            lew.gameASCIIString(msg)
            return lew.getPacket()
        }

        /**
         * Gets an empty stat update.
         *
         * @return The empty stat update packet.
         */
        fun enableActions(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.STAT_CHANGED.value)
            lew.byte(1)
            lew.int(0)
            lew.byte(0)
            return lew.getPacket() //return updatePlayerStats(EMPTY_STATUPDATE, true);
        }

        fun enableReport(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.CLAIM_STATUS_CHANGED.value)
            lew.byte(1)
            return lew.getPacket()
        }

        fun enableTV(): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.byte(SendPacketOpcode.ENABLE_TV.value)
            lew.int(0)
            lew.byte(0)
            return lew.getPacket()
        }

        fun getClock(time: Int): ByteArray { // time in seconds
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CLOCK.value)
            lew.byte(2) // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
            lew.int(time)
            return lew.getPacket()
        }

        fun getClockTime(hour: Int, min: Int, sec: Int): ByteArray { // Current Time
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CLOCK.value)
            lew.byte(1) //Clock-Type
            lew.byte(hour)
            lew.byte(min)
            lew.byte(sec)
            return lew.getPacket()
        }

        /**
         * Gets a gm effect packet (i.e. hide, banned, etc.)
         *
         *
         * Possible values for
         * `type`:<br></br> 0x04: You have successfully blocked access.<br></br>
         * 0x05: The unblocking has been successful.<br></br> 0x06 with Mode 0: You have
         * successfully removed the name from the ranks.<br></br> 0x06 with Mode 1: You
         * have entered an invalid character name.<br></br> 0x10: GM Hide, mode
         * determines whether it is on.<br></br> 0x1E: Mode 0: Failed to send
         * warning Mode 1: Sent warning<br></br> 0x13 with Mode 0: + map id 0x13 with Mode
         * 1: + ch (FF = Unable to find merchant)
         *
         * @param type The type
         * @param mode The mode
         * @return The gm effect packet
         */
        fun getGMEffect(type: Int, mode: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.ADMIN_RESULT.value)
            lew.byte(type)
            lew.byte(mode)
            return lew.getPacket()
        }

        fun getKeyMap(keybindings: Map<Int, KeyBinding>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.KEYMAP.value)
            lew.byte(0)
            for (x in 0..89) {
                val binding = keybindings[Integer.valueOf(x)]
                if (binding != null) {
                    lew.byte(binding.type)
                    lew.int(binding.action)
                } else {
                    lew.byte(0)
                    lew.int(0)
                }
            }
            return lew.getPacket()
        }

        fun <E> getLongMaskD(statUps: List<Pair<Disease, Int>>): Long {
            var mask: Long = 0
            for ((first) in statUps) {
                mask = mask or first.value
            }
            return mask
        }

        /**
         * Gets the response to a relog request.
         *
         * @return The relog response packet.
         */
        fun getRelogResponse(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.RELOG_RESPONSE.value)
            lew.byte(1) //1 O.O Must be more types ):
            return lew.getPacket()
        }


        fun getTime(realTimestamp: Long): Long {
            return when (realTimestamp) {
                -1L -> DEFAULT_TIME
                -2L -> ZERO_TIME
                -3L -> PERMANENT
                else -> realTimestamp * 10000 + FT_UT_OFFSET
            }
        }

        /**
         * Gets a packet telling the client to change maps.
         *
         * @param to         The `GameMap` to warp to.
         * @param spawnPoint The spawn portal number to spawn at.
         * @param chr        The character warping to `to`
         * @return The map change packet.
         */
        fun getWarpToMap(to: GameMap, spawnPoint: Int, chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SET_FIELD.value)
            lew.int(chr.client.channel - 1)
            lew.byte(2) // 맵이동 횟수
            lew.byte(0)
            lew.int(to.mapId)
            lew.byte(spawnPoint)
            lew.short(chr.hp)
            lew.byte(0)
            lew.long(getTime(System.currentTimeMillis()))
            return lew.getPacket()
        }

        fun guideHint(hint: Int): ByteArray {
            val lew = PacketLittleEndianWriter(11)
            lew.byte(SendPacketOpcode.TALK_GUIDE.value)
            lew.byte(1)
            lew.int(hint)
            lew.int(7000)
            return lew.getPacket()
        }

        fun leftKnockBack(): ByteArray {
            val lew = PacketLittleEndianWriter(2)
            lew.byte(SendPacketOpcode.LEFT_KNOCK_BACK.value)
            return lew.getPacket()
        }

        fun lockUI(enable: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.LOCK_UI.value)
            lew.byte(if (enable) 1 else 0)
            return lew.getPacket()
        }

        /**
         * Sends a UI utility. 0x01 - Equipment Inventory. 0x02 - Stat Window. 0x03
         * - Skill Window. 0x05 - Keyboard Settings. 0x06 - Quest window. 0x09 -
         * Monsterbook Window. 0x0A - Char Info 0x0B - Guild BBS 0x12 - Monster
         * Carnival Window 0x16 - Party Search. 0x17 - Item Creation Window. 0x1A -
         * My Ranking O.O 0x1B - Family Window 0x1C - Family Pedigree 0x1D - GM
         * Story Board /funny shet 0x1E - Envelop saying you got mail from an admin.
         * lmfao 0x1F - Medal Window 0x20 - Game Event (???) 0x21 - Invalid Pointer
         * Crash
         *
         * @param ui
         * @return
         */
        fun openUI(ui: Byte): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.OPEN_UI.value)
            lew.byte(ui)
            return lew.getPacket()
        }

        fun remoteChannelChange(ch: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.ENTRUSTED_SHOP_CHECK_RESULT.value) // header.
            lew.byte(0x10)
            lew.int(0) //No idea yet
            lew.byte(ch)
            return lew.getPacket()
        }

        /**
         * Removes TV
         *
         * @return The Remove TV Packet
         */
        fun removeTV(): ByteArray {
            val lew = PacketLittleEndianWriter(2)
            lew.byte(SendPacketOpcode.REMOVE_TV.value)
            return lew.getPacket()
        }

        /**
         * Sends a report response
         *
         *
         * Possible values for
         * `mode`:<br></br> 0: You have succesfully reported the user.<br></br> 1:
         * Unable to locate the user.<br></br> 2: You may only report users 10 times a
         * day.<br></br> 3: You have been reported to the GM's by a user.<br></br> 4: Your
         * request did not go through for unknown reasons. Please try again
         * later.<br></br>
         *
         * @param mode The mode
         * @return Report Reponse packet
         */
        fun reportResponse(mode: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SUE_CHARACTER_RESULT.value)
            lew.byte(mode)
            return lew.getPacket()
        }

        fun resetForcedStats(): ByteArray {
            val lew = PacketLittleEndianWriter(2)
            lew.byte(SendPacketOpcode.FORCED_STAT_RESET.value)
            return lew.getPacket()
        }

        fun retrieveFirstMessage(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.ENTRUSTED_SHOP_CHECK_RESULT.value) // header.
            lew.byte(0x08)
            return lew.getPacket()
        }

        fun sendDuey(operation: Byte, packages: List<DueyPackages>? = null): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PARCEL.value)
            lew.byte(operation)
            if (operation.toInt() == 8) {
                lew.byte(0)
                lew.byte(packages?.size ?: 0)
                if (packages != null) {
                    for (dp in packages) {
                        lew.int(dp.packageId)
                        lew.ASCIIString(dp.sender)
                        for (i in dp.sender.length..12) {
                            lew.byte(0)
                        }
                        lew.int(dp.mesos)
                        lew.long(getTime(dp.sentTimeInMilliseconds()))
                        lew.long(0) // Contains message o____o.
                        for (i in 0..47) {
                            lew.int(Random.nextInt(Int.MAX_VALUE))
                        }
                        lew.int(0)
                        lew.byte(0)
                        if (dp.item != null) {
                            lew.byte(1)
                            addItemInfo(lew, dp.item, zeroPosition = true, leaveOut = true)
                        } else {
                            lew.byte(0)
                        }
                    }
                }
                lew.byte(0)
            }
            return lew.getPacket()
        }

        /**
         * Sends a player hint.
         *
         * @param hint   The hint it's going to send.
         * @param width  How tall the box is going to be.
         * @param height How long the box is going to be.
         * @return The player hint packet.
         */
        fun sendHint(hint: String, width1: Int, height1: Int): ByteArray {
            var width = width1
            var height = height1
            if (width < 1) {
                width = hint.length * 10
                if (width < 40) {
                    width = 40
                }
            }
            if (height < 5) {
                height = 5
            }
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_HINT.value)
            lew.gameASCIIString(hint)
            lew.short(width)
            lew.short(height)
            lew.byte(1)
            return lew.getPacket()
        }

        fun sendMesoLimit(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.TRADE_MONEY_LIMIT.value) //Players under level 15 can only trade 1m per day
            return lew.getPacket()
        }

        /**
         * Sends MTV
         *
         * @param chr      The character shown in TV
         * @param messages The message sent with the TV
         * @param type     The type of TV
         * @param partner  The partner shown with chr
         * @return the SEND_TV packet
         */
        fun sendTV(chr: Character, messages: List<String>, type: Int, partner: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SEND_TV.value)
            lew.byte(if (partner != null) 3 else 1)
            lew.byte(type) //Heart = 2  Star = 1  Normal = 0
            CharacterPacket.addCharLook(lew, chr, false)
            lew.gameASCIIString(chr.name)
            if (partner != null) {
                lew.gameASCIIString(partner.name)
            } else {
                lew.short(0)
            }
            for (i in messages.indices) {
                if (i == 4 && messages[4].length > 15) {
                    lew.gameASCIIString(messages[4].substring(0, 15))
                } else {
                    lew.gameASCIIString(messages[i])
                }
            }
            lew.int(1337) // time limit
            if (partner != null) {
                CharacterPacket.addCharLook(lew, partner, false)
            }
            return lew.getPacket()
        }

        fun sendYellowTip(tip: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SET_WEEK_EVENT_MESSAGE.value)
            lew.byte(0xFF)
            lew.gameASCIIString(tip)
            lew.short(0)
            return lew.getPacket()
        }

        fun showBerserk(cid: Int, skillLevel: Int, berserk: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.int(cid)
            lew.byte(1)
            lew.int(1320006)
            lew.byte(0xA9)
            lew.byte(skillLevel)
            lew.byte(if (berserk) 1 else 0)
            return lew.getPacket()
        }

        fun showBossHP(oid: Int, currHP: Int, maxHP: Int, tagColor: Byte, tagBgColor: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.FIELD_EFFECT.value)
            lew.byte(5)
            lew.int(oid)
            lew.int(currHP)
            lew.int(maxHP)
            lew.byte(tagColor)
            lew.byte(tagBgColor)
            return lew.getPacket()
        }

        fun showBuffEffect(cid: Int, skillId: Int, effectId: Int, direction: Byte = 3): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.int(cid)
            lew.byte(effectId) //buff level
            lew.int(skillId)
            lew.byte(1)
            return lew.getPacket()
        }

        fun showChair(characterId: Int, itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_CHAIR.value)
            lew.int(characterId)
            lew.int(itemId)
            return lew.getPacket()
        }


        @Throws(SQLException::class)
        fun showNotes(list: List<Character.Companion.Note>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MEMO_RESULT.value)
            lew.byte(3)
            lew.byte(list.size)
            for ((id, from, message, timestamp, fame) in list) {
                lew.int(id)
                lew.gameASCIIString("$from ")
                lew.gameASCIIString(message)
                lew.long(getTime(timestamp))
                lew.byte(fame)
            }
            return lew.getPacket()
        }

        /**
         * 6 = Exp did not drop (Safety Charms) 7 = Enter portal sound 8 = Job
         * change 9 = Quest complete 10 = Recovery 14 = Monster book pickup 15 =
         * Equipment levelup 16 = Maker Skill Success 19 = Exp card [500, 200, 50]
         *
         * @param effect
         * @return
         */
        fun showSpecialEffect(effect: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.byte(effect)
            return lew.getPacket()
        }

        fun talkGuide(talk: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.TALK_GUIDE.value)
            lew.byte(0)
            lew.gameASCIIString(talk)
            lew.byte(byteArrayOf(0xC8.toByte(), 0, 0, 0, 0xA0.toByte(), 0x0F.toByte(), 0, 0))
            return lew.getPacket()
        }

        fun updateAreaInfo(area: Int, info: String?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.byte(0x0A) //0x0B in v95
            lew.short(area) //infoNumber
            lew.gameASCIIString(info!!)
            return lew.getPacket()
        }


        fun writeIntMask(lew: PacketLittleEndianWriter, stats: Map<MonsterStatus, Int>) {
            //int firstmask = 0;
            var secondmask = 0
            for (stat in stats.keys) {
                /*if (stat.isFirst()) {
                firstmask |= stat.getValue();
            } else {*/
                secondmask = secondmask or stat.value
                //}
            }
            //lew.int(firstmask);
            lew.int(secondmask)
        }

        fun longMask(lew: PacketLittleEndianWriter, statUps: List<Pair<BuffStat, Int>>) {
            var mask: Long = 0
            for ((first) in statUps) {
                mask = mask or first.value
            }
            lew.long(mask)
        }

        fun longMaskFromList(lew: PacketLittleEndianWriter, statUps: List<BuffStat>) {
            var firstmask: Long = 0
            var secondmask: Long = 0
            for (statup in statUps) {
                if (statup.isFirst) {
                    firstmask = firstmask or statup.value
                } else {
                    secondmask = secondmask or statup.value
                }
            }
            //lew.long(firstmask);
            lew.long(secondmask)
        }
    }
}