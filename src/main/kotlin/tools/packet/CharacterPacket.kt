package tools.packet

import client.*
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.ModifyInventory
import net.SendPacketOpcode
import server.ItemInformationProvider
import server.life.MobSkill
import tools.PacketCreator
import tools.data.output.LittleEndianWriter
import tools.data.output.PacketLittleEndianWriter
import kotlin.random.Random

//Packet for characters, inventory, skill, other personal things...
class CharacterPacket {
    companion object {
        private fun addAttackBody(
            lew: LittleEndianWriter,
            chr: Character,
            skill: Int,
            skillLevel: Int,
            stance: Int,
            numAttackedAndDamage: Int,
            projectile: Int,
            damage: Map<Int, List<Int>?>,
            speed: Int,
            mastery: Int
        ) {
            lew.writeInt(chr.id)
            lew.write(numAttackedAndDamage)
            lew.write(skillLevel)
            if (skillLevel > 0) {
                lew.writeInt(skill)
            }
            lew.write(stance)
            lew.write(speed)
            lew.write(mastery)
            lew.writeInt(projectile)
            for (one in damage.keys) {
                val oneList = damage[one]
                if (oneList != null) {
                    lew.writeInt(one)
                    lew.write(0xFF)
                    if (skill == 4211006) {
                        lew.write(oneList.size)
                    }
                    for (each in oneList) {
                        lew.writeInt(each)
                    }
                }
            }
        }

        fun addCharBox(c: Character, type: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.UPDATE_CHAR_BOX.value)
            lew.writeInt(c.id)
            PacketCreator.addAnnounceBox(lew, c.playerShop, type)
            return lew.getPacket()
        }

        private fun addCharStats(lew: PacketLittleEndianWriter, chr: Character) {
            lew.writeInt(chr.id)
            lew.writeASCIIString(chr.name, 13)
            lew.write(chr.gender)
            lew.write(chr.skinColor.id)
            lew.writeInt(chr.face)
            lew.writeInt(chr.hair)
            lew.writeLong((chr.pet?.uniqueId ?: 0).toLong())
            lew.write(chr.level.toByte())
            lew.writeShort(chr.job.id) // job
            lew.writeShort(chr.str) // str
            lew.writeShort(chr.dex) // dex
            lew.writeShort(chr.int) // int
            lew.writeShort(chr.luk) // luk
            lew.writeShort(chr.hp) // hp (?)
            lew.writeShort(chr.maxHp) // maxhp
            lew.writeShort(chr.mp) // mp (?)
            lew.writeShort(chr.maxMp) // maxmp
            lew.writeShort(chr.remainingAp) // remaining ap
            lew.writeShort(chr.remainingSp) // remaining sp
            lew.writeInt(chr.exp.get()) // current exp
            lew.writeShort(chr.fame) // fame
            lew.writeInt(chr.mapId) // current map id
            lew.write(chr.initialSpawnPoint) // spawnpoint
        }

        fun addCharLook(lew: PacketLittleEndianWriter, chr: Character, mega: Boolean) {
            lew.write(chr.gender)
            lew.write(chr.skinColor.id) // skin color
            lew.writeInt(chr.face) // face
            lew.write(if (mega) 0 else 1)
            lew.writeInt(chr.hair) // hair
            addCharEquips(lew, chr)
        }



        fun addCharacterInfo(lew: PacketLittleEndianWriter, chr: Character) {
            lew.writeShort(-1)
            addCharStats(lew, chr)
            lew.write(chr.buddyList.capacity)
            addInventoryInfo(lew, chr)
            addSkillInfo(lew, chr)
            GameplayPacket.addQuestInfo(lew, chr)
            MiniGamePacket.addMiniGameInfo(lew, chr)
            ItemPacket.addRingInfo(lew, chr)
            CashPacket.addTeleportInfo(lew, chr)
        }

        fun addCharEquips(lew: PacketLittleEndianWriter, chr: Character) {
            val equip = chr.getInventory(InventoryType.EQUIPPED)
            if (equip == null) {
                PacketCreator.logger.error { "chr.getInventory EQUIPPED is null. something gone wrong." }
                return
            }
            val ii = ItemInformationProvider.canWearEquipment(chr, equip.list())
            val myEquip = mutableMapOf<Byte, Int>()
            val maskedEquip = mutableMapOf<Byte, Int>()
            ii.forEach {
                var pos = (it.position * -1).toByte()
                if (pos < 100 && myEquip[pos] == null) {
                    myEquip[pos] = it.itemId
                } else if (pos > 100 && pos != (111).toByte()) {
                    pos = (pos - 100).toByte()
                    val my = myEquip[pos]
                    if (my != null) maskedEquip[pos] = my
                    myEquip[pos] = it.itemId
                } else if (myEquip[pos] != null) {
                    maskedEquip[pos] = it.itemId
                }
            }
            myEquip.forEach { (t, u) ->
                lew.write(t)
                lew.writeInt(u)
            }
            lew.write(0xff)
            maskedEquip.forEach { (t, u) ->
                lew.write(t)
                lew.writeInt(u)
            }
            lew.write(0xff)
            val weapon = equip.getItem(-111)
            lew.writeInt(weapon?.itemId ?: 0)
            lew.writeInt(chr.pet?.itemId ?: 0)
        }

        fun addCharEntry(lew: PacketLittleEndianWriter, chr: Character) {
            addCharStats(lew, chr)
            addCharLook(lew, chr, false)
            if (chr.isGM()) {
                lew.write(0)
            } else {
                lew.write(1) // world rank enabled (next 4 ints are not sent if disabled) Short??
                lew.writeInt(chr.rank) // world rank
                lew.writeInt(chr.rankMove) // move (negative is downwards)
                lew.writeInt(chr.jobRank) // job rank
                lew.writeInt(chr.jobRankMove) // move (negative is downwards)
            }
        }

        private fun addInventoryInfo(plew: PacketLittleEndianWriter, chr: Character) {
            plew.writeInt(chr.meso.get())
            for (i in 1..5) {
                chr.getInventory(InventoryType.getByType(i.toByte()))?.let { plew.write(it.slotLimit) }
            }
            val iv = chr.getInventory(InventoryType.EQUIPPED) ?: return
            val equippedC: Collection<Item> = iv.list()
            val equipped: MutableList<Item> = ArrayList(equippedC.size)
            val equippedCash: MutableList<Item> = ArrayList(equippedC.size)
            for (item in equippedC) {
                if (item.position < -100) {
                    equippedCash.add(item)
                } else {
                    equipped.add(item)
                }
            }
            equipped.sort()
            equippedCash.sort()
            for (item in equipped) {
                ItemPacket.addItemInfo(plew, item, zeroPosition = false, leaveOut = false, trade = false, chr = chr)
            }
            plew.write(0)
            for (item in equippedCash) {
                ItemPacket.addItemInfo(plew, item, zeroPosition = false, leaveOut = false, trade = false, chr = chr)
            }
            plew.write(0)
            for (item in chr.getInventory(InventoryType.EQUIP)?.list() ?: emptyList()) {
                ItemPacket.addItemInfo(plew, item, zeroPosition = false, leaveOut = false, trade = false, chr = chr)
            }
            plew.write(0)
            for (item in chr.getInventory(InventoryType.USE)?.list() ?: emptyList()) {
                ItemPacket.addItemInfo(plew, item, zeroPosition = false, leaveOut = false, trade = false, chr = chr)
            }
            plew.write(0)
            for (item in chr.getInventory(InventoryType.SETUP)?.list() ?: emptyList()) {
                ItemPacket.addItemInfo(plew, item, zeroPosition = false, leaveOut = false, trade = false, chr = chr)
            }
            plew.write(0)
            for (item in chr.getInventory(InventoryType.ETC)?.list() ?: emptyList()) {
                ItemPacket.addItemInfo(plew, item, zeroPosition = false, leaveOut = false, trade = false, chr = chr)
            }
            plew.write(0)
            for (item in chr.getInventory(InventoryType.CASH)?.list() ?: emptyList()) {
                ItemPacket.addItemInfo(plew, item, zeroPosition = false, leaveOut = false, trade = false, chr = chr)
            }
            plew.write(0)
        }

        fun addNewCharEntry(chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.value)
            lew.write(0)
            addCharEntry(lew, chr)
            return lew.getPacket()
        }

        private fun addSkillInfo(plew: PacketLittleEndianWriter, chr: Character) {
            val skills: Map<Skill, Character.Companion.SkillEntry> = chr.skills
            plew.writeShort(skills.size)
            val it = skills.entries.iterator()
            while (it.hasNext()) {
                val (key, value) = it.next()
                plew.writeInt(key.id)
                plew.writeInt(value.skillLevel.toInt())
            }
            plew.writeShort(chr.getAllCoolDowns().size)
            for ((skillId, startTime, length) in chr.getAllCoolDowns()) {
                plew.writeInt(skillId)
                val timeLeft = (length + startTime - System.currentTimeMillis()).toInt()
                plew.writeShort(timeLeft / 1000)
            }
        }

        fun cancelBuff(statups: List<BuffStat>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CANCEL_BUFF.value)
            PacketCreator.writeLongMaskFromList(lew, statups)
            lew.write(1) //?
            return lew.getPacket()
        }

        fun cancelChair(id: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CANCEL_CHAIR.value)
            if (id == -1) {
                lew.write(0)
            } else {
                lew.write(1)
                lew.writeShort(id)
            }
            return lew.getPacket()
        }

        fun cancelDeBuff(mask: Long): ByteArray {
            val lew = PacketLittleEndianWriter(19)
            lew.write(SendPacketOpcode.CANCEL_BUFF.value)
            //lew.writeLong(0);
            lew.writeLong(mask)
            lew.write(0)
            return lew.getPacket()
        }

        fun cancelForeignBuff(cid: Int, statUps: List<BuffStat>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CANCEL_FOREIGN_BUFF.value)
            lew.writeInt(cid)
            PacketCreator.writeLongMaskFromList(lew, statUps)
            return lew.getPacket()
        }

        fun cancelForeignDeBuff(cid: Int, mask: Long): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CANCEL_FOREIGN_BUFF.value)
            lew.writeInt(cid)
            lew.writeLong(mask)
            return lew.getPacket()
        }

        fun catchMonster(mobObjectid: Int, itemId: Int, success: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CATCH_MONSTER.value)
            lew.writeInt(mobObjectid)
            lew.writeInt(itemId)
            lew.write(success)
            return lew.getPacket()
        }

        fun closeRangeAttack(
            chr: Character,
            skill: Int,
            skillLevel: Int,
            stance: Int,
            numAttackedAndDamage: Int,
            damage: Map<Int, List<Int>?>,
            speed: Int,
            mastery: Int
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CLOSE_RANGE_ATTACK.value)
            addAttackBody(lew, chr, skill, skillLevel, stance, numAttackedAndDamage, 0, damage, speed, mastery)
            return lew.getPacket()
        }

        fun charInfo(chr: Character, isSelf: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CHAR_INFO.value)
            lew.writeInt(chr.id)
            lew.write(chr.level.toInt())
            lew.writeShort(chr.job.id)
            lew.writeShort(chr.fame)
            var guildName = ""
            val gs = chr.client.getWorldServer().getGuildSummary(chr.guildId)
            if (chr.guildId > 0 && gs != null) {
                guildName = gs.name
            }
            lew.writeGameASCIIString(guildName)
            val pet = chr.pet
            val inv = chr.getInventory(InventoryType.EQUIPPED)?.getItem((-114).toByte())
            if (pet != null) {
                lew.write(1)
                lew.writeInt(pet.itemId)
                lew.writeGameASCIIString(pet.name)
                lew.write(pet.level)
                lew.writeShort(pet.closeness)
                lew.write(pet.fullness)
                lew.writeShort(-1)
                lew.writeInt(inv?.itemId ?: 0)
            } else {
                lew.write(0)
            }
            lew.write(0)
            return lew.getPacket()
        }

        fun charNameResponse(charName: String, nameUsed: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CHAR_NAME_RESPONSE.value)
            lew.writeGameASCIIString(charName)
            lew.write(if (nameUsed) 1 else 0)
            return lew.getPacket()
        }

        /**
         * state 0 = del ok state 12 = invalid bday state 14 = incorrect pic
         *
         * @param cid
         * @param state
         * @return
         */
        fun deleteCharResponse(cid: Int, state: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.DELETE_CHAR_RESPONSE.value)
            lew.writeInt(cid)
            lew.write(state)
            return lew.getPacket()
        }

        fun facialExpression(from: Character, expression: Int): ByteArray {
            val lew = PacketLittleEndianWriter(10)
            lew.write(SendPacketOpcode.FACIAL_EXPRESSION.value)
            lew.writeInt(from.id)
            lew.writeInt(expression)
            return lew.getPacket()
        }

        fun finishedSort(inv: Int): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.write(SendPacketOpcode.GATHER_ITEM_RESULT.value)
            lew.write(0)
            lew.write(inv)
            return lew.getPacket()
        }

        fun finishedSort2(inv: Int): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.write(SendPacketOpcode.SORT_ITEM_RESULT.value)
            lew.write(0)
            lew.write(inv)
            return lew.getPacket()
        }

        /**
         * Gets character info for a character.
         *
         * @param chr The character to get info about.
         * @return The character info packet.
         */
        fun getCharInfo(chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SET_FIELD.value)
            lew.writeInt(chr.client.channel - 1)
            lew.write(0)
            lew.write(1)
            for (i in 0..2) {
                lew.writeInt(Random.nextInt())
            }
            addCharacterInfo(lew, chr)
            lew.writeLong(PacketCreator.getTime(System.currentTimeMillis()))
            return lew.getPacket()
        }

        /**
         * Gets a packet with a list of characters.
         *
         * @param c        The Client to load characters of.
         * @param serverId The ID of the server requested.
         * @return The character list packet.
         */
        fun getCharList(c: Client, serverId: Int): ByteArray {
            val plew = PacketLittleEndianWriter()
            plew.write(SendPacketOpcode.CHARLIST.value)
            plew.write(0)
            plew.writeInt(0) // 주민등록번호
            val chars = c.loadCharacters(serverId)
            plew.write(chars.size.toByte())
            for (chr in chars) {
                addCharEntry(plew, chr)
            }
            plew.write(2)
            plew.write(0)
            plew.writeInt(c.characterSlots.toInt())
            return plew.getPacket()
        }

        fun getInventoryFull() = modifyInventory(true, listOf())

        /**
         * Gets a packet telling the client to show an EXP increase.
         *
         * @param gain   The amount of EXP gained.
         * @param inChat In the chat box?
         * @param white  White text or yellow?
         * @return The exp gained packet.
         */
        fun getShowExpGain(gain: Int, equip: Int, inChat: Boolean, white: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(3) // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
            lew.writeBool(white)
            lew.writeInt(gain)
            lew.writeBool(inChat)
            lew.writeInt(0) // monster book bonus (Bonus Event Exp)
            lew.writeShort(0) //Weird stuff
            lew.writeInt(0) //wedding bonus
            lew.write(0) //0 = party bonus, 1 = Bonus Event party Exp () x0
            lew.writeInt(0) // party bonus
            lew.writeInt(equip) //equip bonus
            lew.writeInt(0) //Internet Cafe Bonus
            lew.writeInt(0) //Rainbow Week Bonus
            if (inChat) {
                lew.write(0)
            }
            return lew.getPacket()
        }

        fun getShowInventoryFull() = getShowInventoryStatus(0xff)

        fun getShowInventoryStatus(mode: Int): ByteArray {
            val plew = PacketLittleEndianWriter()
            plew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            plew.write(0)
            plew.write(mode)
            plew.writeInt(0)
            plew.writeInt(0)
            return plew.getPacket()
        }

        /**
         * Gets a packet telling the client to show a meso gain.
         *
         * @param gain   How many mesos gained.
         * @param inChat Show in the chat window?
         * @return The meso gain packet.
         */
        fun getShowMesoGain(gain: Int, inChat: Boolean = false): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            if (!inChat) {
                lew.write(0)
                lew.write(1)
            } else {
                lew.write(5)
            }
            lew.writeInt(gain)
            lew.writeShort(0)
            return lew.getPacket()
        }

        fun getStorage(npcId: Int, slots: Byte, items: Collection<Item>, meso: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.STORAGE.value)
            lew.write(0x15)
            lew.writeInt(npcId)
            lew.write(slots)
            lew.writeShort(0x7E)
            lew.writeInt(meso)
            lew.writeShort(0)
            lew.write(0)
            lew.write(items.size.toByte())
            for (item in items) {
                ItemPacket.addItemInfo(lew, item, true, true)
            }
            lew.write(0)
            return lew.getPacket()
        }

        /*
            * 0x0A = Inv full
            * 0x0B = You do not have enough mesos
            * 0x0C = One-Of-A-Kind error
        */
        fun getStorageError(i: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.STORAGE.value)
            lew.write(0x0A)
            return lew.getPacket()
        }

        fun giveDeBuff(statUps: List<Pair<Disease, Int>>, skill: MobSkill): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GIVE_BUFF.value)
            val mask = PacketCreator.getLongMaskD<Any>(statUps)
            lew.writeLong(mask)
            for ((_, second) in statUps) {
                lew.writeShort(second.toShort().toInt())
                lew.writeShort(skill.skillId)
                lew.writeShort(skill.skillLevel)
                lew.writeShort(skill.duration.toInt())
            }
            //lew.writeShort(0); // ??? wk charges have 600 here o.o
            lew.writeShort(900) //Delay
            lew.write(1)
            return lew.getPacket()
        }

        /**
         * It is important that statups is in the correct order (see decleration
         * order in BuffStat) since this method doesn't do automagical
         * reordering.
         *
         * @param buffId
         * @param buffLength
         * @param statUps
         * @return
         */
        fun giveBuff(buffId: Int, buffLength: Int, statUps: List<Pair<BuffStat, Int>>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GIVE_BUFF.value)
            //boolean special = false;
            PacketCreator.writeLongMask(lew, statUps)
            for ((_, second) in statUps) {
                //if (statup.getLeft().equals(BuffStat.MONSTER_RIDING)
                //        || statup.getLeft().equals(BuffStat.HOMING_BEACON)) {
                //    special = true;
                //}
                lew.writeShort(second.toShort().toInt())
                lew.writeInt(buffId)
                lew.writeShort(buffLength)
            }
            lew.writeShort(0)
            lew.write(0)
            //lew.writeInt(0);
            //lew.write(0);
            //lew.writeInt(statups.get(0).getRight()); //Homing beacon ...
            //if (special) {
            //    lew.skip(3);
            //}
            return lew.getPacket()
        }

        fun giveForeignBuff(cid: Int, statUps: List<Pair<BuffStat, Int>>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.writeInt(cid)
            PacketCreator.writeLongMask(lew, statUps)
            for ((_, second) in statUps) {
                lew.writeShort(second.toShort().toInt())
            }
            lew.writeInt(0)
            lew.writeShort(0)
            return lew.getPacket()
        }

        fun giveForeignDash(cid: Int, buffid: Int, time: Int, statUps: List<Pair<BuffStat, Int>>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.writeInt(cid)
            PacketCreator.writeLongMask(lew, statUps)
            lew.writeShort(0)
            for ((_, second) in statUps) {
                lew.writeInt(second.toShort().toInt())
                lew.writeInt(buffid)
                lew.skip(5)
                lew.writeShort(time)
            }
            lew.writeShort(0)
            lew.write(2)
            return lew.getPacket()
        }

        fun giveForeignDeBuff(cid: Int, statUps: List<Pair<Disease, Int>>, skill: MobSkill): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.writeInt(cid)
            val mask = PacketCreator.getLongMaskD<Any>(statUps)
            lew.writeLong(mask)
            for (i in statUps.indices) {
                lew.writeShort(skill.skillId)
                lew.writeShort(skill.skillLevel)
            }
            lew.writeShort(0) // same as give_buff
            lew.writeShort(900) //Delay
            return lew.getPacket()
        }

        fun giveForeignInfusion(cid: Int, speed: Int, duration: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.writeInt(cid)
            lew.writeLong(BuffStat.SPEED_INFUSION.value)
            lew.writeLong(0)
            lew.writeShort(0)
            lew.writeInt(speed)
            lew.writeInt(5121009)
            lew.writeLong(0)
            lew.writeInt(duration)
            lew.writeShort(0)
            return lew.getPacket()
        }

        fun givePirateBuff(statUps: List<Pair<BuffStat, Int>>, buffId: Int, duration: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GIVE_BUFF.value)
            PacketCreator.writeLongMask(lew, statUps)
            lew.writeShort(0)
            for ((_, second) in statUps) {
                lew.writeInt(second.toShort().toInt())
                lew.writeInt(buffId)
                lew.skip(5)
                lew.writeShort(duration)
            }
            lew.skip(3)
            return lew.getPacket()
        }

        fun magicAttack(
            chr: Character,
            skill: Int,
            skillLevel: Int,
            stance: Int,
            numAttackedAndDamage: Int,
            damage: Map<Int, List<Int>?>,
            charge: Int,
            speed: Int,
            mastery: Int
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MAGIC_ATTACK.value)
            addAttackBody(
                lew, chr, skill, skillLevel,
                stance, numAttackedAndDamage, 0, damage, speed, mastery
            )
            if (charge != -1) {
                lew.writeInt(charge)
            }
            return lew.getPacket()
        }

        fun mesoStorage(slots: Byte, meso: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.STORAGE.value)
            lew.write(0x10)
            lew.write(slots)
            lew.writeShort(2)
            lew.writeInt(meso)
            return lew.getPacket()
        }

        fun modifyInventory(updateTick: Boolean, mods: List<ModifyInventory>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.INVENTORY_OPERATION.value)
            lew.writeBool(updateTick) //if~
            lew.write(mods.size) //v4
            var addMovement = 0
            for (mod in mods) {
                lew.write(mod.mode) //v6
                lew.write(mod.getInventoryType()) //v8
                when (mod.mode) {
                    0 -> { //add item
                        lew.writeShort(mod.getPosition().toInt())
                        ItemPacket.addItemInfo(lew, mod.item, zeroPosition = true, leaveOut = true)
                    }
                    1 -> { //update quantity
                        lew.writeShort(mod.getPosition().toInt())
                        lew.writeShort(mod.getQuantity().toInt())
                    }
                    2 -> { //move
                        lew.writeShort(mod.getPosition().toInt())
                        lew.writeShort(mod.oldPosition.toInt())
                        if (mod.getPosition() < 0 || mod.oldPosition < 0) {
                            addMovement = if (mod.oldPosition < 0) 1 else 2
                        }
                    }
                    3 -> { //remove
                        if (mod.getPosition() < 0) {
                            if (mod.oldPosition < 0) {
                                lew.writeShort(mod.oldPosition.toInt())
                            }
                            addMovement = 2
                        }
                        lew.writeShort(mod.getPosition().toInt())
                    }
                }
                //mod.clear()
            }
            lew.write(addMovement)
            return lew.getPacket()
        }

        fun rangedAttack(
            chr: Character,
            skill: Int,
            skillLevel: Int,
            stance: Int,
            numAttackedAndDamage: Int,
            projectile: Int,
            damage: MutableMap<Int, List<Int>?>,
            speed: Int,
            mastery: Int
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.RANGED_ATTACK.value)
            addAttackBody(
                lew,
                chr,
                skill,
                skillLevel,
                stance,
                numAttackedAndDamage,
                projectile,
                damage,
                speed,
                mastery
            )
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun removeCharBox(c: Character): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.write(SendPacketOpcode.UPDATE_CHAR_BOX.value)
            lew.writeInt(c.id)
            lew.write(0)
            return lew.getPacket()
        }

        fun showForeignEffect(cid: Int, effect: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.writeInt(cid)
            lew.write(effect)
            return lew.getPacket()
        }

        fun showMagnet(mobId: Int, success: Byte): ByteArray { // Monster Magnet
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_MAGNET.value)
            lew.writeInt(mobId)
            lew.write(success)
            lew.skip(10) //Mmmk
            return lew.getPacket()
        }

        /**
         * @param cid
         * @param mount
         * @return
         */
        fun showMonsterRiding(cid: Int, mount: Mount?): ByteArray {
            if (mount == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.writeInt(cid)
            lew.writeLong(BuffStat.MONSTER_RIDING.value) //Thanks?
            lew.writeLong(0)
            lew.writeShort(0)
            lew.writeInt(mount.itemId)
            lew.writeInt(mount.skillId)
            lew.writeInt(0) //Server Tick value.
            lew.writeShort(0)
            lew.write(0) //Times you have been buffed
            return lew.getPacket()
        }

        fun showOwnBerserk(skillLevel: Int, berserk: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.write(1)
            lew.writeInt(1320006)
            lew.write(0xA9)
            lew.write(skillLevel)
            lew.write(if (berserk) 1 else 0)
            return lew.getPacket()
        }

        fun showOwnBuffEffect(skillId: Int, effectId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.write(effectId)
            lew.writeInt(skillId)
            lew.write(1)
            return lew.getPacket()
        }

        fun showOwnRecovery(heal: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.write(0x0A)
            lew.write(heal)
            return lew.getPacket()
        }

        fun showRecovery(cid: Int, amount: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.writeInt(cid)
            lew.write(0x0A)
            lew.write(amount)
            return lew.getPacket()
        }

        fun skillCancel(from: Character?, skillId: Int): ByteArray {
            if (from == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CANCEL_SKILL_EFFECT.value)
            lew.writeInt(from.id)
            lew.writeInt(skillId)
            return lew.getPacket()
        }

        fun skillCoolDown(sid: Int, time: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.COOLDOWN.value)
            lew.writeInt(sid)
            lew.writeShort(time) //Int in v97
            return lew.getPacket()
        }

        fun skillEffect(from: Character, skillId: Int, level: Int, flags: Byte, speed: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SKILL_EFFECT.value)
            lew.writeInt(from.id)
            lew.writeInt(skillId)
            lew.write(level)
            lew.write(flags)
            lew.write(speed)
            return lew.getPacket()
        }

        fun storeStorage(slots: Byte, type: InventoryType, items: Collection<Item>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.STORAGE.value)
            lew.write(0xB)
            lew.write(slots)
            lew.writeShort(type.getBitfieldEncoding())
            lew.write(items.size)
            for (item in items) {
                ItemPacket.addItemInfo(lew, item, true, true)
            }
            return lew.getPacket()
        }

        fun summonAttack(cid: Int, summonSkillId: Int, newStance: Byte, monsterOid: Int, damage: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SUMMON_ATTACK.value)
            lew.writeInt(cid)
            lew.writeInt(summonSkillId)
            lew.write(newStance)
            lew.writeInt(monsterOid) // oid
            lew.write(6) // who knows
            lew.writeInt(damage) // damage
            return lew.getPacket()
        }

        fun summonSkill(cid: Int, summonSkillId: Int, newStance: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SUMMON_SKILL.value)
            lew.writeInt(cid)
            lew.writeInt(summonSkillId)
            lew.write(newStance)
            return lew.getPacket()
        }

        fun takeOutStorage(slots: Byte, type: InventoryType, items: List<Item>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.STORAGE.value)
            lew.write(0x8)
            lew.write(slots)
            lew.writeShort(type.getBitfieldEncoding())
            lew.write(items.size)
            for (item in items) {
                ItemPacket.addItemInfo(lew, item, true, true)
            }
            return lew.getPacket()
        }

        fun updateCharLook(chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.UPDATE_CHAR_LOOK.value)
            lew.writeInt(chr.id)
            lew.write(1)
            addCharLook(lew, chr, false)
            ItemPacket.addRingLook(lew, chr, true)
            ItemPacket.addRingLook(lew, chr, false)
            //addMarriageRingLook(lew, chr);
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun updateGender(chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.write(SendPacketOpcode.SET_GENDER.value)
            lew.write(chr.gender)
            return lew.getPacket()
        }

        fun updateInventorySlotLimit(type: Int, newLimit: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.INVENTORY_GROW.value)
            lew.write(type)
            lew.write(newLimit)
            lew.writeLong(0) // 1.55 ++
            return lew.getPacket()
        }

        fun updateSkill(skillId: Int, level: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.UPDATE_SKILLS.value)
            lew.write(1)
            lew.writeShort(1)
            lew.writeInt(skillId)
            lew.writeInt(level)
            //lew.writeInt(masterlevel);
            lew.write(1)
            return lew.getPacket()
        }

        fun updateMount(charId: Int, mount: Mount?, levelup: Boolean): ByteArray {
            if (mount == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SET_TAMING_MOB_INFO.value)
            lew.writeInt(charId)
            lew.writeInt(mount.level)
            lew.writeInt(mount.exp)
            lew.writeInt(mount.tiredness)
            lew.write(if (levelup) 1.toByte() else 0.toByte())
            return lew.getPacket()
        }

        /**
         * Gets an update for specified stats.
         *
         * @param stats The list of stats to update.
         * @param itemReaction Result of an item reaction(?)
         * @return The stat update packet.
         */
        fun updatePlayerStats(stats: List<Pair<CharacterStat, Int>>, itemReaction: Boolean = false): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.STAT_CHANGED.value)
            lew.write(if (itemReaction) 1 else 0)
            var updateMask = 0 //v2 + 8288
            for ((first) in stats) {
                updateMask = updateMask or first.value
            }
            val newStats = if (stats.size > 1) stats.sortedBy { it.first.value } else stats
            lew.writeInt(updateMask)
            for ((first, second) in newStats) {
                if (first.value >= 1) {
                    when {
                        first.value == 0x01 -> lew.writeShort(second.toShort().toInt())
                        first.value == 0x08 -> lew.writeLong(0) //diff packet
                        first.value <= 0x04 -> lew.writeInt(second)
                        first.value < 0x20 -> lew.write(second.toShort().toInt())
                        first.value < 0xffff -> lew.writeShort(second.toShort().toInt())
                        else -> lew.writeInt(second)
                    }
                }
            }
            lew.write(0) //Pet. diff packet
            return lew.getPacket()
        }
    }
}