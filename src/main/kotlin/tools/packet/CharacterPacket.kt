package tools.packet

import client.*
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.ModifyInventory
import net.SendPacketOpcode
import server.ItemInformationProvider
import server.life.MobSkill
import tools.PacketCreator
import tools.PacketCreator.Companion.packetWriter
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
            lew.int(chr.id)
            lew.byte(numAttackedAndDamage)
            lew.byte(skillLevel)
            if (skillLevel > 0) {
                lew.int(skill)
            }
            lew.byte(stance)
            lew.byte(speed)
            lew.byte(mastery)
            lew.int(projectile)
            for (one in damage.keys) {
                val oneList = damage[one]
                if (oneList != null) {
                    lew.int(one)
                    lew.byte(0xFF)
                    if (skill == 4211006) {
                        lew.byte(oneList.size)
                    }
                    for (each in oneList) {
                        lew.int(each)
                    }
                }
            }
        }

        fun addCharBox(c: Character, type: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.UPDATE_CHAR_BOX.value)
            lew.int(c.id)
            PacketCreator.addAnnounceBox(lew, c.playerShop, type)
            return lew.getPacket()
        }

        private fun addCharStats(chr: Character) = packetWriter {
            int(chr.id)
            ASCIIString(chr.name, 13)
            byte(chr.gender)
            byte(chr.skinColor.id)
            int(chr.face)
            int(chr.hair)
            long((chr.pet?.uniqueId ?: 0).toLong())
            byte(chr.level.toByte())
            short(chr.job.id) // job
            short(chr.str) // str
            short(chr.dex) // dex
            short(chr.int) // int
            short(chr.luk) // luk
            short(chr.hp) // hp (?)
            short(chr.maxHp) // maxhp
            short(chr.mp) // mp (?)
            short(chr.maxMp) // maxmp
            short(chr.remainingAp) // remaining ap
            short(chr.remainingSp) // remaining sp
            int(chr.exp.get()) // current exp
            short(chr.fame) // fame
            int(chr.mapId) // current map id
            byte(chr.initialSpawnPoint) // spawnpoint
        }

        fun addCharLook(chr: Character, mega: Boolean) = packetWriter {
            byte(chr.gender)
            byte(chr.skinColor.id) // skin color
            int(chr.face) // face
            byte(if (mega) 0 else 1)
            int(chr.hair) // hair
            byte(addCharEquips(chr))
        }



        fun addCharacterInfo(chr: Character) = packetWriter {
            short(-1)
            byte(addCharStats(chr))
            byte(chr.buddyList.capacity)
            byte(addInventoryInfo(chr))
            byte(addSkillInfo(chr))
            byte(GameplayPacket.addQuestInfo(chr))
            byte(MiniGamePacket.addMiniGameInfo(chr))
            byte(ItemPacket.addRingInfo(chr))
            byte(CashPacket.addTeleportInfo(chr))
        }

        fun addCharEquips(chr: Character) = packetWriter {
            val equip = chr.getInventory(InventoryType.EQUIPPED)
            if (equip == null) {
                PacketCreator.logger.error { "chr.getInventory EQUIPPED is null. something gone wrong." }
                return@packetWriter
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
                byte(t)
                int(u)
            }
            byte(0xff)
            maskedEquip.forEach { (t, u) ->
                byte(t)
                int(u)
            }
            byte(0xff)
            val weapon = equip.getItem(-111)
            int(weapon?.itemId ?: 0)
            int(chr.pet?.itemId ?: 0)
        }

        fun addCharEntry(chr: Character) = packetWriter {
            byte(addCharStats(chr))
            byte(addCharLook(chr, false))
            if (chr.isGM()) {
                this.byte(0)
            } else {
                byte(1) // world rank enabled (next 4 ints are not sent if disabled) Short??
                int(chr.rank) // world rank
                int(chr.rankMove) // move (negative is downwards)
                int(chr.jobRank) // job rank
                int(chr.jobRankMove) // move (negative is downwards)
            }
        }

        private fun addInventoryInfo(chr: Character) = packetWriter {
            int(chr.meso.get())
            for (i in 1..5) {
                chr.getInventory(InventoryType.getByType(i.toByte()))?.let { byte(it.slotLimit) }
            }
            val iv = chr.getInventory(InventoryType.EQUIPPED) ?: return@packetWriter
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
                byte(ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = false, chr = chr))
            }
            byte(0)
            for (item in equippedCash) {
                byte(ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = false, chr = chr))
            }
            byte(0)
            for (item in chr.getInventory(InventoryType.EQUIP)?.list() ?: emptyList()) {
                byte(ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = false, chr = chr))
            }
            byte(0)
            for (item in chr.getInventory(InventoryType.USE)?.list() ?: emptyList()) {
                byte(ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = false, chr = chr))
            }
            byte(0)
            for (item in chr.getInventory(InventoryType.SETUP)?.list() ?: emptyList()) {
                byte(ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = false, chr = chr))
            }
            byte(0)
            for (item in chr.getInventory(InventoryType.ETC)?.list() ?: emptyList()) {
                byte(ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = false, chr = chr))
            }
            byte(0)
            for (item in chr.getInventory(InventoryType.CASH)?.list() ?: emptyList()) {
                byte(ItemPacket.addItemInfo(item, zeroPosition = false, leaveOut = false, trade = false, chr = chr))
            }
            byte(0)
        }

        fun addNewCharEntry(chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.value)
            lew.byte(0)
            lew.byte(addCharEntry(chr))
            return lew.getPacket()
        }

        private fun addSkillInfo(chr: Character) = packetWriter {
            val skills: Map<Skill, Character.Companion.SkillEntry> = chr.skills
            short(skills.size)
            val it = skills.entries.iterator()
            while (it.hasNext()) {
                val (key, value) = it.next()
                int(key.id)
                int(value.skillLevel.toInt())
            }
            short(chr.getAllCoolDowns().size)
            for ((skillId, startTime, length) in chr.getAllCoolDowns()) {
                int(skillId)
                val timeLeft = (length + startTime - System.currentTimeMillis()).toInt()
                short(timeLeft / 1000)
            }
        }

        fun cancelBuff(statups: List<BuffStat>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CANCEL_BUFF.value)
            PacketCreator.longMaskFromList(lew, statups)
            lew.byte(1) //?
            return lew.getPacket()
        }

        fun cancelChair(id: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CANCEL_CHAIR.value)
            if (id == -1) {
                lew.byte(0)
            } else {
                lew.byte(1)
                lew.short(id)
            }
            return lew.getPacket()
        }

        fun cancelDeBuff(mask: Long): ByteArray {
            val lew = PacketLittleEndianWriter(19)
            lew.byte(SendPacketOpcode.CANCEL_BUFF.value)
            //lew.long(0);
            lew.long(mask)
            lew.byte(0)
            return lew.getPacket()
        }

        fun cancelForeignBuff(cid: Int, statUps: List<BuffStat>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CANCEL_FOREIGN_BUFF.value)
            lew.int(cid)
            PacketCreator.longMaskFromList(lew, statUps)
            return lew.getPacket()
        }

        fun cancelForeignDeBuff(cid: Int, mask: Long): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CANCEL_FOREIGN_BUFF.value)
            lew.int(cid)
            lew.long(mask)
            return lew.getPacket()
        }

        fun catchMonster(mobObjectid: Int, itemId: Int, success: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CATCH_MONSTER.value)
            lew.int(mobObjectid)
            lew.int(itemId)
            lew.byte(success)
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
            lew.byte(SendPacketOpcode.CLOSE_RANGE_ATTACK.value)
            addAttackBody(lew, chr, skill, skillLevel, stance, numAttackedAndDamage, 0, damage, speed, mastery)
            return lew.getPacket()
        }

        fun charInfo(chr: Character, isSelf: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CHAR_INFO.value)
            lew.int(chr.id)
            lew.byte(chr.level.toInt())
            lew.short(chr.job.id)
            lew.short(chr.fame)
            var guildName = ""
            val gs = chr.client.getWorldServer().getGuildSummary(chr.guildId)
            if (chr.guildId > 0 && gs != null) {
                guildName = gs.name
            }
            lew.gameASCIIString(guildName)
            val pet = chr.pet
            val inv = chr.getInventory(InventoryType.EQUIPPED)?.getItem((-114).toByte())
            if (pet != null) {
                lew.byte(1)
                lew.int(pet.itemId)
                lew.gameASCIIString(pet.name)
                lew.byte(pet.level)
                lew.short(pet.closeness)
                lew.byte(pet.fullness)
                lew.short(-1)
                lew.int(inv?.itemId ?: 0)
            } else {
                lew.byte(0)
            }
            lew.byte(0)
            return lew.getPacket()
        }

        fun charNameResponse(charName: String, nameUsed: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CHAR_NAME_RESPONSE.value)
            lew.gameASCIIString(charName)
            lew.byte(if (nameUsed) 1 else 0)
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
            lew.byte(SendPacketOpcode.DELETE_CHAR_RESPONSE.value)
            lew.int(cid)
            lew.byte(state)
            return lew.getPacket()
        }

        fun facialExpression(from: Character, expression: Int): ByteArray {
            val lew = PacketLittleEndianWriter(10)
            lew.byte(SendPacketOpcode.FACIAL_EXPRESSION.value)
            lew.int(from.id)
            lew.int(expression)
            return lew.getPacket()
        }

        fun finishedSort(inv: Int): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.byte(SendPacketOpcode.GATHER_ITEM_RESULT.value)
            lew.byte(0)
            lew.byte(inv)
            return lew.getPacket()
        }

        fun finishedSort2(inv: Int): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.byte(SendPacketOpcode.SORT_ITEM_RESULT.value)
            lew.byte(0)
            lew.byte(inv)
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
            lew.byte(SendPacketOpcode.SET_FIELD.value)
            lew.int(chr.client.channel - 1)
            lew.byte(0)
            lew.byte(1)
            for (i in 0..2) {
                lew.int(Random.nextInt())
            }
            lew.byte(addCharacterInfo(chr))
            lew.long(PacketCreator.getTime(System.currentTimeMillis()))
            return lew.getPacket()
        }

        /**
         * Gets a packet with a list of characters.
         *
         * @param c        The Client to load characters of.
         * @param serverId The ID of the server requested.
         * @return The character list packet.
         */
        fun getCharList(c: Client, serverId: Int) = packetWriter(SendPacketOpcode.CHARLIST) {
            byte(0)
            int(0) // 주민등록번호
            val chars = c.loadCharacters(serverId)
            byte(chars.size.toByte())
            for (chr in chars) {
                byte(addCharEntry(chr))
            }
            byte(2)
            byte(0)
            int(c.characterSlots.toInt())
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
            lew.byte(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.byte(3) // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
            lew.bool(white)
            lew.int(gain)
            lew.bool(inChat)
            lew.int(0) // monster book bonus (Bonus Event Exp)
            lew.short(0) //Weird stuff
            lew.int(0) //wedding bonus
            lew.byte(0) //0 = party bonus, 1 = Bonus Event party Exp () x0
            lew.int(0) // party bonus
            lew.int(equip) //equip bonus
            lew.int(0) //Internet Cafe Bonus
            lew.int(0) //Rainbow Week Bonus
            if (inChat) {
                lew.byte(0)
            }
            return lew.getPacket()
        }

        fun getShowInventoryFull() = getShowInventoryStatus(0xff)

        fun getShowInventoryStatus(mode: Int): ByteArray {
            val plew = PacketLittleEndianWriter()
            plew.byte(SendPacketOpcode.SHOW_STATUS_INFO.value)
            plew.byte(0)
            plew.byte(mode)
            plew.int(0)
            plew.int(0)
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
            lew.byte(SendPacketOpcode.SHOW_STATUS_INFO.value)
            if (!inChat) {
                lew.byte(0)
                lew.byte(1)
            } else {
                lew.byte(5)
            }
            lew.int(gain)
            lew.short(0)
            return lew.getPacket()
        }

        fun getStorage(npcId: Int, slots: Byte, items: Collection<Item>, meso: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.STORAGE.value)
            lew.byte(0x15)
            lew.int(npcId)
            lew.byte(slots)
            lew.short(0x7E)
            lew.int(meso)
            lew.short(0)
            lew.byte(0)
            lew.byte(items.size.toByte())
            for (item in items) {
                ItemPacket.addItemInfo(item, true, true)
            }
            lew.byte(0)
            return lew.getPacket()
        }

        /*
            * 0x0A = Inv full
            * 0x0B = You do not have enough mesos
            * 0x0C = One-Of-A-Kind error
        */
        fun getStorageError(i: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.STORAGE.value)
            lew.byte(0x0A)
            return lew.getPacket()
        }

        fun giveDeBuff(statUps: List<Pair<Disease, Int>>, skill: MobSkill): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GIVE_BUFF.value)
            val mask = PacketCreator.getLongMaskD<Any>(statUps)
            lew.long(mask)
            for ((_, second) in statUps) {
                lew.short(second.toShort().toInt())
                lew.short(skill.skillId)
                lew.short(skill.skillLevel)
                lew.short(skill.duration.toInt())
            }
            //lew.short(0); // ??? wk charges have 600 here o.o
            lew.short(900) //Delay
            lew.byte(1)
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
            lew.byte(SendPacketOpcode.GIVE_BUFF.value)
            //boolean special = false;
            PacketCreator.longMask(lew, statUps)
            for ((_, second) in statUps) {
                //if (statup.getLeft().equals(BuffStat.MONSTER_RIDING)
                //        || statup.getLeft().equals(BuffStat.HOMING_BEACON)) {
                //    special = true;
                //}
                lew.short(second.toShort().toInt())
                lew.int(buffId)
                lew.short(buffLength)
            }
            lew.short(0)
            lew.byte(0)
            //lew.int(0);
            //lew.byte(0);
            //lew.int(statups.get(0).getRight()); //Homing beacon ...
            //if (special) {
            //    lew.skip(3);
            //}
            return lew.getPacket()
        }

        fun giveForeignBuff(cid: Int, statUps: List<Pair<BuffStat, Int>>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.int(cid)
            PacketCreator.longMask(lew, statUps)
            for ((_, second) in statUps) {
                lew.short(second.toShort().toInt())
            }
            lew.int(0)
            lew.short(0)
            return lew.getPacket()
        }

        fun giveForeignDash(cid: Int, buffid: Int, time: Int, statUps: List<Pair<BuffStat, Int>>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.int(cid)
            PacketCreator.longMask(lew, statUps)
            lew.short(0)
            for ((_, second) in statUps) {
                lew.int(second.toShort().toInt())
                lew.int(buffid)
                lew.skip(5)
                lew.short(time)
            }
            lew.short(0)
            lew.byte(2)
            return lew.getPacket()
        }

        fun giveForeignDeBuff(cid: Int, statUps: List<Pair<Disease, Int>>, skill: MobSkill): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.int(cid)
            val mask = PacketCreator.getLongMaskD<Any>(statUps)
            lew.long(mask)
            for (i in statUps.indices) {
                lew.short(skill.skillId)
                lew.short(skill.skillLevel)
            }
            lew.short(0) // same as give_buff
            lew.short(900) //Delay
            return lew.getPacket()
        }

        fun giveForeignInfusion(cid: Int, speed: Int, duration: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.int(cid)
            lew.long(BuffStat.SPEED_INFUSION.value)
            lew.long(0)
            lew.short(0)
            lew.int(speed)
            lew.int(5121009)
            lew.long(0)
            lew.int(duration)
            lew.short(0)
            return lew.getPacket()
        }

        fun givePirateBuff(statUps: List<Pair<BuffStat, Int>>, buffId: Int, duration: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.GIVE_BUFF.value)
            PacketCreator.longMask(lew, statUps)
            lew.short(0)
            for ((_, second) in statUps) {
                lew.int(second.toShort().toInt())
                lew.int(buffId)
                lew.skip(5)
                lew.short(duration)
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
            lew.byte(SendPacketOpcode.MAGIC_ATTACK.value)
            addAttackBody(
                lew, chr, skill, skillLevel,
                stance, numAttackedAndDamage, 0, damage, speed, mastery
            )
            if (charge != -1) {
                lew.int(charge)
            }
            return lew.getPacket()
        }

        fun mesoStorage(slots: Byte, meso: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.STORAGE.value)
            lew.byte(0x10)
            lew.byte(slots)
            lew.short(2)
            lew.int(meso)
            return lew.getPacket()
        }

        fun modifyInventory(updateTick: Boolean, mods: List<ModifyInventory>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.INVENTORY_OPERATION.value)
            lew.bool(updateTick) //if~
            lew.byte(mods.size) //v4
            var addMovement = 0
            for (mod in mods) {
                lew.byte(mod.mode) //v6
                lew.byte(mod.getInventoryType()) //v8
                when (mod.mode) {
                    0 -> { //add item
                        lew.short(mod.getPosition().toInt())
                        ItemPacket.addItemInfo(mod.item, zeroPosition = true, leaveOut = true)
                    }
                    1 -> { //update quantity
                        lew.short(mod.getPosition().toInt())
                        lew.short(mod.getQuantity().toInt())
                    }
                    2 -> { //move
                        lew.short(mod.getPosition().toInt())
                        lew.short(mod.oldPosition.toInt())
                        if (mod.getPosition() < 0 || mod.oldPosition < 0) {
                            addMovement = if (mod.oldPosition < 0) 1 else 2
                        }
                    }
                    3 -> { //remove
                        if (mod.getPosition() < 0) {
                            if (mod.oldPosition < 0) {
                                lew.short(mod.oldPosition.toInt())
                            }
                            addMovement = 2
                        }
                        lew.short(mod.getPosition().toInt())
                    }
                }
                //mod.clear()
            }
            lew.byte(addMovement)
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
            lew.byte(SendPacketOpcode.RANGED_ATTACK.value)
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
            lew.int(0)
            return lew.getPacket()
        }

        fun removeCharBox(c: Character): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.byte(SendPacketOpcode.UPDATE_CHAR_BOX.value)
            lew.int(c.id)
            lew.byte(0)
            return lew.getPacket()
        }

        fun showForeignEffect(cid: Int, effect: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.int(cid)
            lew.byte(effect)
            return lew.getPacket()
        }

        fun showMagnet(mobId: Int, success: Byte): ByteArray { // Monster Magnet
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_MAGNET.value)
            lew.int(mobId)
            lew.byte(success)
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
            lew.byte(SendPacketOpcode.GIVE_FOREIGN_BUFF.value)
            lew.int(cid)
            lew.long(BuffStat.MONSTER_RIDING.value) //Thanks?
            lew.long(0)
            lew.short(0)
            lew.int(mount.itemId)
            lew.int(mount.skillId)
            lew.int(0) //Server Tick value.
            lew.short(0)
            lew.byte(0) //Times you have been buffed
            return lew.getPacket()
        }

        fun showOwnBerserk(skillLevel: Int, berserk: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.byte(1)
            lew.int(1320006)
            lew.byte(0xA9)
            lew.byte(skillLevel)
            lew.byte(if (berserk) 1 else 0)
            return lew.getPacket()
        }

        fun showOwnBuffEffect(skillId: Int, effectId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.byte(effectId)
            lew.int(skillId)
            lew.byte(1)
            return lew.getPacket()
        }

        fun showOwnRecovery(heal: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.byte(0x0A)
            lew.byte(heal)
            return lew.getPacket()
        }

        fun showRecovery(cid: Int, amount: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.int(cid)
            lew.byte(0x0A)
            lew.byte(amount)
            return lew.getPacket()
        }

        fun skillCancel(from: Character?, skillId: Int): ByteArray {
            if (from == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CANCEL_SKILL_EFFECT.value)
            lew.int(from.id)
            lew.int(skillId)
            return lew.getPacket()
        }

        fun skillCoolDown(sid: Int, time: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.COOLDOWN.value)
            lew.int(sid)
            lew.short(time) //Int in v97
            return lew.getPacket()
        }

        fun skillEffect(from: Character, skillId: Int, level: Int, flags: Byte, speed: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SKILL_EFFECT.value)
            lew.int(from.id)
            lew.int(skillId)
            lew.byte(level)
            lew.byte(flags)
            lew.byte(speed)
            return lew.getPacket()
        }

        fun storeStorage(slots: Byte, type: InventoryType, items: Collection<Item>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.STORAGE.value)
            lew.byte(0xB)
            lew.byte(slots)
            lew.short(type.getBitfieldEncoding())
            lew.byte(items.size)
            for (item in items) {
                ItemPacket.addItemInfo(item, true, true)
            }
            return lew.getPacket()
        }

        fun summonAttack(cid: Int, summonSkillId: Int, newStance: Byte, monsterOid: Int, damage: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SUMMON_ATTACK.value)
            lew.int(cid)
            lew.int(summonSkillId)
            lew.byte(newStance)
            lew.int(monsterOid) // oid
            lew.byte(6) // who knows
            lew.int(damage) // damage
            return lew.getPacket()
        }

        fun summonSkill(cid: Int, summonSkillId: Int, newStance: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SUMMON_SKILL.value)
            lew.int(cid)
            lew.int(summonSkillId)
            lew.byte(newStance)
            return lew.getPacket()
        }

        fun takeOutStorage(slots: Byte, type: InventoryType, items: List<Item>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.STORAGE.value)
            lew.byte(0x8)
            lew.byte(slots)
            lew.short(type.getBitfieldEncoding())
            lew.byte(items.size)
            for (item in items) {
                ItemPacket.addItemInfo(item, true, true)
            }
            return lew.getPacket()
        }

        fun updateCharLook(chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.UPDATE_CHAR_LOOK.value)
            lew.int(chr.id)
            lew.byte(1)
            addCharLook(chr, false)
            ItemPacket.addRingLook(lew, chr, true)
            ItemPacket.addRingLook(lew, chr, false)
            //addMarriageRingLook(lew, chr);
            lew.int(0)
            return lew.getPacket()
        }

        fun updateGender(chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.SET_GENDER.value)
            lew.byte(chr.gender)
            return lew.getPacket()
        }

        fun updateInventorySlotLimit(type: Int, newLimit: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.INVENTORY_GROW.value)
            lew.byte(type)
            lew.byte(newLimit)
            lew.long(0) // 1.55 ++
            return lew.getPacket()
        }

        fun updateSkill(skillId: Int, level: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.UPDATE_SKILLS.value)
            lew.byte(1)
            lew.short(1)
            lew.int(skillId)
            lew.int(level)
            //lew.int(masterlevel);
            lew.byte(1)
            return lew.getPacket()
        }

        fun updateMount(charId: Int, mount: Mount?, levelup: Boolean): ByteArray {
            if (mount == null) return ByteArray(0)
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SET_TAMING_MOB_INFO.value)
            lew.int(charId)
            lew.int(mount.level)
            lew.int(mount.exp)
            lew.int(mount.tiredness)
            lew.byte(if (levelup) 1.toByte() else 0.toByte())
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
            lew.byte(SendPacketOpcode.STAT_CHANGED.value)
            lew.byte(if (itemReaction) 1 else 0)
            var updateMask = 0 //v2 + 8288
            for ((first) in stats) {
                updateMask = updateMask or first.value
            }
            val newStats = if (stats.size > 1) stats.sortedBy { it.first.value } else stats
            lew.int(updateMask)
            for ((first, second) in newStats) {
                if (first.value >= 1) {
                    when {
                        first.value == 0x01 -> lew.short(second.toShort().toInt())
                        first.value == 0x08 -> lew.long(0) //diff packet
                        first.value <= 0x04 -> lew.int(second)
                        first.value < 0x20 -> lew.byte(second.toShort().toInt())
                        first.value < 0xffff -> lew.short(second.toShort().toInt())
                        else -> lew.int(second)
                    }
                }
            }
            lew.byte(0) //Pet. diff packet
            return lew.getPacket()
        }
    }
}