package tools.packet

import client.BuffStat
import client.Character
import client.inventory.InventoryType
import client.status.MonsterStatus
import client.status.MonsterStatusEffect
import constants.ItemConstants
import net.SendPacketOpcode
import server.life.Monster
import server.maps.Mist
import server.maps.PlayerNPCs
import server.maps.Reactor
import server.maps.Summon
import server.movement.LifeMovementFragment
import tools.PacketCreator
import tools.data.output.LittleEndianWriter
import tools.data.output.PacketLittleEndianWriter
import java.awt.Point

//Packet for game playing. maps, monsters, ...
class GameplayPacket {
    companion object {
        fun addQuestInfo(lew: PacketLittleEndianWriter, chr: Character) {
            lew.writeShort(chr.getStartedQuestsSize())
            chr.getStartedQuests().forEach {
                lew.writeShort(it.quest.id.toInt())
                lew.writeGameASCIIString(it.getQuestData())
                if (it.quest.infoNumber > 0) {
                    lew.writeShort(it.quest.infoNumber.toInt())
                    lew.writeGameASCIIString(it.medalProgress.toString())
                }
            }
            val completed = chr.getCompletedQuests()
            lew.writeShort(completed.size)
            completed.forEach {
                lew.writeShort(it.quest.id.toInt())
                lew.writeLong(PacketCreator.getTime(it.completionTime))
            }
        }

        fun addQuestTimeLimit(quest: Short, time: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.UPDATE_QUEST_INFO.value)
            lew.write(6)
            lew.writeShort(1) //Size but meh, when will there be 2 at the same time? And it won't even replace the old one :)
            lew.writeShort(quest.toInt())
            lew.writeInt(time)
            return lew.getPacket()
        }

        fun applyMonsterStatus(oid: Int, mse: MonsterStatusEffect): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.APPLY_MONSTER_STATUS.value)
            lew.writeInt(oid)
            PacketCreator.writeIntMask(lew, mse.stati)
            for ((_, value) in mse.stati) {
                lew.writeShort(value)
                if (mse.monsterSkill) {
                    mse.mobSkill?.skillId?.let { lew.writeShort(it) }
                    mse.mobSkill?.skillLevel?.let { lew.writeShort(it) }
                } else {
                    lew.writeInt(mse.skill?.id ?: -1)
                }
                lew.writeShort(0)
            }
            lew.writeShort(900)
            lew.write(1)
            return lew.getPacket()
        }

        fun cancelMonsterStatus(oid: Int, stats: Map<MonsterStatus, Int?>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CANCEL_MONSTER_STATUS.value)
            lew.writeInt(oid)
            // lew.writeLong(0);
            lew.writeInt(0)
            var mask = 0
            for (stat in stats.keys) {
                mask = mask or stat.value
            }
            lew.writeInt(mask)
            lew.writeInt(0)
            //lew.write(0);
            return lew.getPacket()
        }

        /**
         * @param quest
         * @return
         */
        fun completeQuest(quest: Short, time: Long): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(1)
            lew.writeShort(quest.toInt())
            lew.write(2)
            lew.writeLong(PacketCreator.getTime(time))
            return lew.getPacket()
        }

        /**
         * Gets a control monster packet.
         *
         * @param life     The monster to give control to.
         * @param newSpawn Is it a new spawn?
         * @param aggro    Aggressive monster?
         * @return The monster control packet.
         */
        fun controlMonster(life: Monster, newSpawn: Boolean, aggro: Boolean): ByteArray {
            return spawnMonsterInternal(life, true, newSpawn, aggro, 0, false)
        }

        fun damageMonster(oid: Int, damage: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.DAMAGE_MONSTER.value)
            lew.writeInt(oid)
            lew.write(0)
            lew.writeInt(damage)
            lew.write(0)
            lew.write(0)
            lew.write(0)
            return lew.getPacket()
        }

        fun damagePlayer(
            skill: Int,
            monsterIdFrom: Int,
            cid: Int,
            damage: Int,
            fake: Int,
            direction: Int,
            pgmr: Boolean,
            pgmr1: Int,
            isPg: Boolean,
            oid: Int,
            posX: Int,
            posY: Int
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.DAMAGE_PLAYER.value)
            lew.writeInt(cid)
            lew.writeInt(0)
            lew.write(monsterIdFrom)
            lew.writeInt(damage)
            lew.write(direction)
            if (pgmr) {
                lew.write(pgmr1)
                lew.write(if (isPg) 1 else 0)
                lew.writeInt(oid)
                lew.write(6)
                lew.writeShort(posX)
                lew.writeShort(posY)
                lew.write(0)
            } else {
                lew.write(0)
            }
            lew.writeInt(damage)
            if (fake > 0) {
                lew.writeInt(fake)
            }
            return lew.getPacket()
        }

        fun damageSummon(cid: Int, summonSkillId: Int, damage: Int, unkByte: Int, monsterIdFrom: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.DAMAGE_SUMMON.value)
            lew.writeInt(cid)
            lew.writeInt(summonSkillId)
            lew.write(unkByte)
            lew.writeInt(damage)
            lew.writeInt(monsterIdFrom)
            lew.write(0)
            return lew.getPacket()
        }

        fun destroyReactor(reactor: Reactor): ByteArray {
            val lew = PacketLittleEndianWriter()
            val pos = reactor.position
            lew.write(SendPacketOpcode.REACTOR_DESTROY.value)
            lew.writeInt(reactor.objectId)
            lew.write(reactor.state)
            lew.writePos(pos)
            return lew.getPacket()
        }

        fun environmentChange(env: String, mode: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.FIELD_EFFECT.value)
            lew.write(mode)
            lew.writeGameASCIIString(env)
            return lew.getPacket()
        }

        /**
         * @param quest
         * @return
         */
        fun forfeitQuest(quest: Short): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(1)
            lew.writeShort(quest.toInt())
            lew.writeShort(0)
            lew.write(0)
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

        fun killMonster(oid: Int, animation: Boolean): ByteArray {
            return killMonster(oid, if (animation) 1 else 0)
        }

        /**
         * Gets a packet telling the client that a monster was killed.
         *
         * @param oid The objectID of the killed monster.
         * @param animation 0 = dissapear, 1 = fade out, 2+ = special
         * @return The kill monster packet.
         */
        fun killMonster(oid: Int, animation: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.KILL_MONSTER.value)
            lew.writeInt(oid)
            lew.write(animation)
            return lew.getPacket()
        }

        /**
         * Makes a monster invisible for Ariant PQ.
         *
         * @param life
         * @return
         */
        fun makeMonsterInvisible(life: Monster): ByteArray {
            return spawnMonsterInternal(life,
                requestController = true,
                newSpawn = false,
                aggro = false,
                effect = 0,
                makeInvisible = true
            )
        }

        /**
         * Makes a monster previously spawned as non-targetable, targetable.
         *
         * @param life The mob to make targetable.
         * @return The packet to make the mob targetable.
         */
        fun makeMonsterReal(life: Monster): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_MONSTER.value)
            lew.writeInt(life.objectId)
            lew.write(5)
            lew.writeInt(life.id)
            lew.writeInt(0)
            lew.writePos(life.position)
            lew.write(life.stance)
            lew.writeShort(0) //life.getStartFh()
            lew.writeShort(life.fh ?: 0)
            lew.writeShort(-1)
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun mapEffect(path: String?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.FIELD_EFFECT.value)
            lew.write(3)
            lew.writeGameASCIIString(path!!)
            return lew.getPacket()
        }

        fun mapSound(path: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.FIELD_EFFECT.value)
            lew.write(4)
            lew.writeGameASCIIString(path)
            return lew.getPacket()
        }

        fun mobDamageMobFriendly(mob: Monster, damage: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.DAMAGE_MONSTER.value)
            lew.writeInt(mob.objectId)
            lew.write(1) // direction ?
            lew.writeInt(damage)
            var remainingHp = mob.hp - damage
            if (remainingHp <= 0) {
                remainingHp = 0
                mob.map?.removeMapObject(mob)
            }
            mob.hp = remainingHp
            lew.writeInt(remainingHp)
            lew.writeInt(mob.stats.hp)
            return lew.getPacket()
        }

        fun moveMonster(
            useSkill: Byte,
            skill: Byte,
            skill1: Int,
            skill2: Byte,
            skill3: Byte,
            skill4: Byte,
            oid: Int,
            startPos: Point,
            moves: List<LifeMovementFragment>
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MOVE_MONSTER.value)
            lew.writeInt(oid)
            lew.write(useSkill)
            lew.write(skill)
            lew.write(skill1)
            lew.write(skill2)
            lew.write(skill3)
            lew.write(skill4)
            lew.writePos(startPos)
            serializeMovementList(lew, moves)
            return lew.getPacket()
        }

        /**
         * Gets a response to a move monster packet.
         *
         * @param objectId The ObjectID of the monster being moved.
         * @param moveId The movement ID.
         * @param currentMp The current MP of the monster.
         * @param useSkills Can the monster use skills?
         * @param skillId The skill ID for the monster to use.
         * @param skillLevel The level of the skill to use.
         * @return The move response packet.
         */
        fun moveMonsterResponse(
            objectId: Int,
            moveId: Short,
            currentMp: Int,
            useSkills: Boolean,
            skillId: Int = 0,
            skillLevel: Int = 0
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MOVE_MONSTER_RESPONSE.value)
            lew.writeInt(objectId)
            lew.writeShort(moveId.toInt())
            lew.writeBool(useSkills)
            lew.writeShort(currentMp)
            lew.write(skillId)
            lew.write(skillLevel)
            return lew.getPacket()
        }

        fun movePet(cid: Int, pid: Int, moves: List<LifeMovementFragment>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MOVE_PET.value)
            lew.writeInt(cid)
            //lew.write(slot);
            lew.writeInt(pid)
            serializeMovementList(lew, moves)
            return lew.getPacket()
        }

        fun movePlayer(cid: Int, moves: List<LifeMovementFragment>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MOVE_PLAYER.value)
            lew.writeInt(cid)
            lew.writeInt(0)
            serializeMovementList(lew, moves)
            return lew.getPacket()
        }

        fun moveSummon(cid: Int, summonSkill: Int, startPos: Point, moves: List<LifeMovementFragment>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MOVE_SUMMON.value)
            lew.writeInt(cid)
            lew.writeInt(summonSkill)
            lew.writePos(startPos)
            serializeMovementList(lew, moves)
            return lew.getPacket()
        }

        fun musicChange(song: String) = environmentChange(song, 6)

        fun playPortalSound() = PacketCreator.showSpecialEffect(7)

        fun playSound(sound: String) = environmentChange(sound, 4)

        fun questExpire(quest: Short): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.UPDATE_QUEST_INFO.value)
            lew.write(0x0F)
            lew.writeShort(quest.toInt())
            return lew.getPacket()
        }

        fun questProgress(id: Short, process: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(1)
            lew.writeShort(id.toInt())
            lew.write(1)
            lew.writeGameASCIIString(process)
            return lew.getPacket()
        }

        /**
         * Gets a packet to remove a door.
         *
         * @param oid  The door's ID.
         * @param town
         * @return The remove door packet.
         */
        fun removeDoor(oid: Int, town: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            if (town) {
                lew.write(SendPacketOpcode.SPAWN_PORTAL.value)
                lew.writeInt(999999999)
                lew.writeInt(999999999)
            } else {
                lew.write(SendPacketOpcode.REMOVE_DOOR.value)
                lew.write(0)
                lew.writeInt(oid)
            }
            return lew.getPacket()
        }

        /**
         * animation: 0 - expire<br></br> 1 - without animation<br></br> 2 - pickup<br></br> 4 -
         * explode<br></br> cid is ignored for 0 and 1.<br></br><br></br>Flagging pet as true
         * will make a pet pick up the item.
         *
         * @param oid
         * @param animation
         * @param cid
         * @param pet
         * @param slot
         * @return
         */
        fun removeItemFromMap(oid: Int, animation: Int, cid: Int, pet: Boolean = false, slot: Int = 0): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.value)
            lew.write(animation) // expire
            lew.writeInt(oid)
            if (animation >= 2) {
                lew.writeInt(cid)
                if (pet) lew.write(slot)
            }
            return lew.getPacket()
        }

        fun removeMapEffect(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.BLOW_WEATHER.value)
            lew.write(0)
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun removeMist(oid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.REMOVE_MIST.value)
            lew.writeInt(oid)
            return lew.getPacket()
        }

        fun removePlayerFromMap(cid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.value)
            lew.writeInt(cid)
            return lew.getPacket()
        }

        fun removeQuestTimeLimit(quest: Short): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.UPDATE_QUEST_INFO.value)
            lew.write(7)
            lew.writeShort(1) //Position
            lew.writeShort(quest.toInt())
            return lew.getPacket()
        }

        /**
         * Gets a packet to remove a special map object.
         *
         * @param summon
         * @param animated Animated removal?
         * @return The packet removing the object.
         */
        fun removeSummon(summon: Summon, animated: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.REMOVE_SPECIAL_MAPOBJECT.value)
            lew.writeInt(summon.owner.id)
            lew.writeInt(summon.skill)
            lew.write(if (animated) 4 else 1) // ?
            return lew.getPacket()
        }

        fun serializeMovementList(lew: LittleEndianWriter, moves: List<LifeMovementFragment>) {
            lew.write(moves.size)
            for (move in moves) {
                move.serialize(lew)
            }
        }

        fun showEffect(effect: String) = environmentChange(effect, 3)

        fun showForcedEquip(team: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.FORCED_MAP_EQUIP.value)
            if (team > -1) {
                lew.write(team) // 00 = red, 01 = blue
            }
            return lew.getPacket()
        }

        fun showInfo(path: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.write(0x17)
            lew.writeGameASCIIString(path)
            lew.writeInt(1)
            return lew.getPacket()
        }

        fun showInfoText(text: String?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(9)
            lew.writeGameASCIIString(text!!)
            return lew.getPacket()
        }

        fun showIntro(path: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.write(0x12)
            lew.writeGameASCIIString(path)
            return lew.getPacket()
        }

        /**
         * @param oid
         * @param remHpPercentage
         * @return
         */
        fun showMonsterHP(oid: Int, remHpPercentage: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_MONSTER_HP.value)
            lew.writeInt(oid)
            lew.write(remHpPercentage)
            return lew.getPacket()
        }

        fun showWheelsLeft(left: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.write(0x15)
            lew.write(left)
            return lew.getPacket()
        }

        /**
         * Gets a packet to spawn a door.
         *
         * @param oid  The door's object ID.
         * @param pos  The position of the door.
         * @param town
         * @return The remove door packet.
         */
        fun spawnDoor(oid: Int, pos: Point?, town: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter(11)
            lew.write(SendPacketOpcode.SPAWN_DOOR.value)
            lew.writeBool(town)
            lew.writeInt(oid)
            lew.writePos(pos!!)
            return lew.getPacket()
        }

        /**
         * Handles monsters not being targettable, such as Zakum's first body.
         *
         * @param life   The mob to spawn as non-targettable.
         * @param effect The effect to show when spawning.
         * @return The packet to spawn the mob as non-targettable.
         */
        fun spawnFakeMonster(life: Monster, effect: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_MONSTER_CONTROL.value)
            lew.write(1)
            lew.writeInt(life.objectId)
            lew.write(5)
            lew.writeInt(life.id) ///setlocal
            lew.writeInt(0)
            lew.writePos(life.position)
            lew.write(life.stance)
            lew.writeShort(0) //life.getStartFh()
            lew.writeShort(life.fh ?: 0)
            if (effect > 0) {
                lew.write(effect)
                lew.write(0)
                lew.writeShort(0)
            }
            lew.writeShort(-2)
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun spawnGuide(spawn: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.write(SendPacketOpcode.SPAWN_GUIDE.value)
            lew.write(if (spawn) 1 else 0)
            return lew.getPacket()
        }

        fun spawnMist(oid: Int, ownerCid: Int, skill: Int, level: Int, mist: Mist): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_MIST.value)
            lew.writeInt(oid)
            lew.write(if (mist.isMobMist) 1 else if (mist.isPoisonMist) 0 else 2)
            lew.writeInt(skill)
            lew.write(level)
            lew.writeShort(mist.skillDelay) // Skill delay
            lew.writeInt(mist.mistPosition.x)
            lew.writeInt(mist.mistPosition.y)
            lew.writeInt(mist.mistPosition.x + mist.mistPosition.width)
            lew.writeInt(mist.mistPosition.y + mist.mistPosition.height)
            lew.writeInt(0)
            return lew.getPacket()
        }

        /**
         * Gets a spawn monster packet.
         *
         * @param life     The monster to spawn.
         * @param newSpawn Is it a new spawn?
         * @return The spawn monster packet.
         */
        fun spawnMonster(life: Monster, newSpawn: Boolean): ByteArray {
            return spawnMonsterInternal(life, false, newSpawn, false, 0, false)
        }

        /**
         * Gets a spawn monster packet.
         *
         * @param life     The monster to spawn.
         * @param newSpawn Is it a new spawn?
         * @param effect   The spawn effect.
         * @return The spawn monster packet.
         */
        fun spawnMonster(life: Monster, newSpawn: Boolean, effect: Int): ByteArray {
            return spawnMonsterInternal(life, false, newSpawn, false, effect, false)
        }

        /**
         * Internal function to handler monster spawning and controlling.
         *
         * @param life              The mob to perform operations with.
         * @param requestController Requesting control of mob?
         * @param newSpawn          New spawn (fade in?)
         * @param aggro             Aggressive mob?
         * @param effect            The spawn effect to use.
         * @return The spawn/control packet.
         */
        private fun spawnMonsterInternal(
            life: Monster,
            requestController: Boolean,
            newSpawn: Boolean,
            aggro: Boolean,
            effect: Int,
            makeInvisible: Boolean
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            if (makeInvisible) {
                lew.write(SendPacketOpcode.SPAWN_MONSTER_CONTROL.value)
                lew.write(0)
                lew.writeInt(life.objectId)
                return lew.getPacket()
            }
            if (requestController) {
                lew.write(SendPacketOpcode.SPAWN_MONSTER_CONTROL.value)
                lew.write(if (aggro) 2 else 1)
            } else {
                lew.write(SendPacketOpcode.SPAWN_MONSTER.value)
            }
            lew.writeInt(life.objectId) //v7
            lew.write(if (life.controller == null) 5 else 1)
            lew.writeInt(life.id) //v4
            lew.writeInt(0)
            lew.writePos(life.position)
            lew.write(life.stance)
            lew.writeShort(0) //Origin FH //life.getStartFh()
            lew.writeShort(life.fh ?: 0)
            if (effect > 0) {
                lew.write(effect)
                lew.write(0)
                lew.writeShort(0)
                if (effect == 15) {
                    lew.write(0)
                }
            }
            lew.writeShort(if (newSpawn) -2 else -1)
            lew.writeInt(0)
            return lew.getPacket()
        }


        fun spawnPlayerNpc(npc: PlayerNPCs): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.value)
            lew.write(1)
            lew.writeInt(npc.objectId)
            lew.writeInt(npc.npcId)
            lew.writeShort(npc.position.x)
            lew.writeShort(npc.cy)
            lew.write(1)
            lew.writeShort(npc.fh)
            lew.writeShort(npc.rx0)
            lew.writeShort(npc.rx1)
            lew.write(1)
            return lew.getPacket()
        }

        /**
         * Gets a packet spawning a player as a map object to other clients.
         *
         * @param chr The character to spawn to other clients.
         * @return The spawn player packet.
         */
        fun spawnPlayerMapObject(chr: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_PLAYER.value)
            lew.writeInt(chr.id)
            //lew.write(chr.getLevel());
            lew.writeGameASCIIString(chr.name)
            if (chr.guildId < 1) {
                lew.writeGameASCIIString("")
                lew.write(ByteArray(6))
            } else {
                val gs = chr.client.getWorldServer().getGuildSummary(chr.guildId)
                if (gs != null) {
                    lew.writeGameASCIIString(gs.name)
                    lew.writeShort(gs.logoBG.toInt())
                    lew.write(gs.logoBGColor)
                    lew.writeShort(gs.logo.toInt())
                    lew.write(gs.logoColor)
                } else {
                    lew.writeGameASCIIString("")
                    lew.write(ByteArray(6))
                }
            }
            var buffMask: Long = 0
            if (chr.getBuffedValue(BuffStat.DARKSIGHT) != null && !chr.hidden) {
                buffMask = buffMask or BuffStat.DARKSIGHT.value
            }
            if (chr.getBuffedValue(BuffStat.MONSTER_RIDING) != null) {
                buffMask = buffMask or BuffStat.MONSTER_RIDING.value
            }
            if (chr.getBuffedValue(BuffStat.SHADOWPARTNER) != null) {
                buffMask = buffMask or BuffStat.SHADOWPARTNER.value
            }
            lew.writeLong(buffMask)
            CharacterPacket.addCharLook(lew, chr, false)
            lew.writeInt(chr.getInventory(InventoryType.CASH)?.countById(5110000) ?: 0)
            lew.writeInt(chr.itemEffect)
            lew.writeInt(if (ItemConstants.getInventoryType(chr.chair) == InventoryType.SETUP) chr.chair else 0)
            lew.writePos(chr.position)
            lew.write(chr.stance)
            lew.writeShort(0)
            if (chr.pet != null) {
                CashPacket.addPetInfo(lew, chr.pet)
                lew.writeInt(0)
            } else {
                lew.write(0)
            }
            if (chr.playerShop != null && chr.playerShop?.isOwner(chr) == true) {
                if (chr.playerShop?.hasFreeSlot() == true) {
                    PacketCreator.addAnnounceBox(lew, chr.playerShop, chr.playerShop?.visitors?.size ?: 0)
                } else {
                    PacketCreator.addAnnounceBox(lew, chr.playerShop, 1)
                }
            } else if (chr.miniGame != null && chr.miniGame?.isOwner(chr) == true) {
                var gameType = 1
                if (chr.miniGame?.gameType == "matchcard") gameType = 2
                if (chr.miniGame?.hasFreeSlot() == true) {
                    PacketCreator.addAnnounceBox(lew, chr.miniGame, gameType, chr.miniGame?.locker, 0, 1, 0)
                } else {
                    PacketCreator.addAnnounceBox(lew, chr.miniGame, gameType, chr.miniGame?.locker, 0, 2, 1)
                }
            } else {
                lew.write(0)
            }
            /*if (chr.getChalkboard() != null) {
                lew.write(1);
                lew.writeAsciiString(chr.getChalkboard());
            } else {
                lew.write(0);
            }*/
            ItemPacket.addRingLook(lew, chr, true)
            ItemPacket.addRingLook(lew, chr, false)
            //addMarriageRingLook(lew, chr);
            //lew.write(0); // ??
            lew.write(chr.eventTeam) //only needed in specific fields
            return lew.getPacket()
        }

        /**
         * Gets a packet to spawn a portal.
         *
         * @param townId   The ID of the town the portal goes to.
         * @param targetId The ID of the target.
         * @param pos      Where to put the portal.
         * @return The portal spawn packet.
         */
        fun spawnPortal(townId: Int, targetId: Int, pos: Point?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_PORTAL.value)
            lew.writeInt(townId)
            lew.writeInt(targetId)
            if (pos != null) {
                lew.writePos(pos)
            }
            return lew.getPacket()
        }

        // is there a way to spawn reactors non-animated?
        fun spawnReactor(reactor: Reactor): ByteArray {
            val lew = PacketLittleEndianWriter()
            val pos = reactor.position
            lew.write(SendPacketOpcode.REACTOR_SPAWN.value)
            lew.writeInt(reactor.objectId)
            lew.writeInt(reactor.rid)
            lew.write(reactor.state)
            lew.writePos(pos)
            //lew.writeShort(0);
            lew.write(0)
            return lew.getPacket()
        }

        /**
         * Gets a packet to spawn a special map object.
         *
         * @param summon
         * @param animated Animated spawn?
         * @return The spawn packet for the map object.
         */
        fun spawnSummon(summon: Summon, animated: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_SPECIAL_MAPOBJECT.value)
            lew.writeInt(summon.owner.id)
            lew.writeInt(summon.skill)
            lew.write(summon.skillLevel)
            lew.writePos(summon.position)
            lew.write(4)
            lew.writeShort(0)
            lew.write(summon.movementType.value) // 0 = don't move, 1 = follow (4th mage summons?), 2/4 = only tele follow, 3 = bird follow
            lew.write(if (summon.isPuppet()) 0 else 1) // 0 and the summon can't attack - but puppets don't attack with 1 either ^.-
            lew.write(if (animated) 0 else 1)
            return lew.getPacket()
        }

        fun startMapEffect(msg: String, itemId: Int, active: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.BLOW_WEATHER.value)
            // lew.write(active ? 0 : 1);
            lew.writeInt(itemId)
            if (active) {
                lew.writeGameASCIIString(msg)
            }
            return lew.getPacket()
        }

        /**
         * Gets a stop control monster packet.
         *
         * @param oid The ObjectID of the monster to stop controlling.
         * @return The stop control monster packet.
         */
        fun stopControllingMonster(oid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_MONSTER_CONTROL.value)
            lew.write(0)
            lew.writeInt(oid)
            return lew.getPacket()
        }

        fun triggerReactor(reactor: Reactor, stance: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            val pos = reactor.position
            lew.write(SendPacketOpcode.REACTOR_HIT.value)
            lew.writeInt(reactor.objectId)
            lew.write(reactor.state)
            lew.writePos(pos)
            lew.writeShort(stance)
            lew.write(0)
            lew.write(5) // frame delay, set to 5 since there doesn't appear to be a fixed formula for it
            return lew.getPacket()
        }

        fun updateQuest(quest: Short, status: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(1)
            lew.writeShort(quest.toInt())
            lew.write(1)
            lew.writeGameASCIIString(status)
            return lew.getPacket()
        }

        fun updateQuestFinish(quest: Short, npc: Int, nextQuest: Short): ByteArray { //Check
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.UPDATE_QUEST_INFO.value) //0xF2 in v95
            lew.write(8) //0x0A in v95
            lew.writeShort(quest.toInt())
            lew.writeInt(npc)
            lew.writeShort(nextQuest.toInt())
            return lew.getPacket()
        }
    }
}