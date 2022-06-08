package tools.packet

import client.Character
import client.inventory.Equip
import client.inventory.Item
import net.SendPacketOpcode
import server.ItemInformationProvider
import server.maps.MapItem
import tools.PacketCreator
import tools.data.output.PacketLittleEndianWriter
import java.awt.Point

//Packet for item
class ItemPacket {
    companion object {
        fun addExpirationTime(lew: PacketLittleEndianWriter, time: Long) {
            lew.writeLong(PacketCreator.getTime(time))
        }

        fun addRingInfo(plew: PacketLittleEndianWriter, chr: Character) {
            plew.writeShort(chr.crushRings.size)
            for (ring in chr.crushRings) {
                plew.writeInt(ring.partnerId)
                plew.writeASCIIString(ring.partnerName, 13)
                plew.writeInt(ring.ringId)
                plew.writeInt(0)
                plew.writeInt(ring.partnerRingId)
                plew.writeInt(0)
            }
            plew.writeShort(chr.friendshipRings.size)
            for (ring in chr.friendshipRings) {
                plew.writeInt(ring.partnerId)
                plew.writeASCIIString(ring.partnerName, 13)
                plew.writeInt(ring.ringId)
                plew.writeInt(0)
                plew.writeInt(ring.partnerRingId)
                plew.writeInt(0)
                plew.writeInt(ring.itemId)
            }
        }

        fun addRingLook(lew: PacketLittleEndianWriter, chr: Character, crush: Boolean) {
            val rings = if (crush) {
                chr.getCrushRingsSorted()
            } else {
                chr.friendshipRings
            }
            var yes = false
            for (ring in rings) {
                if (ring.equipped) {
                    if (!yes) {
                        yes = true
                        lew.write(1)
                    }
                    lew.writeInt(ring.ringId)
                    lew.writeInt(0)
                    lew.writeInt(ring.partnerRingId)
                    lew.writeInt(0)
                    lew.writeInt(ring.itemId)
                }
            }
            if (!yes) {
                lew.write(0)
            }
        }

        fun addPetItemInfo(plew: PacketLittleEndianWriter, item: Item) {
            val pet = item.pet ?: return
            plew.writeLong(0)
            plew.writeASCIIString(pet.name, 13)
            plew.write(pet.level)
            plew.writeShort(pet.closeness)
            plew.write(pet.fullness)
            addExpirationTime(plew, item.expiration)
            plew.writeShort(0)
            plew.writeShort(-1)
        }

        fun addItemInfo(lew: PacketLittleEndianWriter, item: Item) {
            addItemInfo(lew, item, true, true)
        }

        fun addItemInfo(lew: PacketLittleEndianWriter, item: Item, zeroPosition: Boolean = true, leaveOut: Boolean = true, trade: Boolean = false, chr: Character? = null) {
            val hasUniqueId = ItemInformationProvider.isCash(item.itemId)
            var isRing = false
            var pos = item.position
            if (zeroPosition) {
                if (!leaveOut) lew.write(0)
            } else {
                if (pos <= -1) {
                    pos = (pos * -1).toByte()
                    if (pos in 101..999) {
                        pos = (pos - 100).toByte()
                    }
                }
                lew.write(pos)
            }
            lew.write(if (item.pet != null) 3 else item.getType())
            lew.writeInt(item.itemId)
            lew.writeBool(hasUniqueId)
            if (hasUniqueId) {
                lew.writeLong(if (item.pet != null) item.petId.toLong() else item.cashId.toLong())
            }
            if (item.pet != null) { // Pet
                addPetItemInfo(lew, item)
                return
            } else {
                addExpirationTime(lew, item.expiration)
                if (item.getType() == 1) {
                    item as Equip
                    isRing = item.ringId > -1
                    lew.write(item.upgradeSlots)
                    lew.write(item.level)
                    lew.writeShort(item.str.toInt())
                    lew.writeShort(item.dex.toInt())
                    lew.writeShort(item.int.toInt())
                    lew.writeShort(item.luk.toInt())
                    lew.writeShort(item.hp.toInt())
                    lew.writeShort(item.mp.toInt())
                    lew.writeShort(item.watk.toInt())
                    lew.writeShort(item.matk.toInt())
                    lew.writeShort(item.wdef.toInt())
                    lew.writeShort(item.mdef.toInt())
                    lew.writeShort(item.acc.toInt())
                    lew.writeShort(item.avoid.toInt())
                    lew.writeShort(item.hands.toInt())
                    lew.writeShort(item.speed.toInt())
                    lew.writeShort(item.jump.toInt())
                    lew.writeGameASCIIString(item.owner)
                    lew.writeShort(item.flag)
                } else {
                    lew.writeShort(item.quantity.toInt())
                    lew.writeGameASCIIString(item.owner)
                    lew.writeShort(item.flag)
                }
            }
        }

        fun changePetName(chr: Character, newName: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PET_NAMECHANGE.value)
            lew.writeInt(chr.id)
            //lew.write(0);
            lew.writeGameASCIIString(newName)
            lew.write(0)
            return lew.getPacket()
        }

        fun dropItemFromMapObject(drop: MapItem, dropFrom: Point?, dropTo: Point?, mod: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.value)
            lew.write(mod)
            lew.writeInt(drop.objectId)
            lew.writeBool(drop.meso > 0) // 1 mesos, 0 item, 2 and above all item meso bag,
            lew.writeInt(drop.getItemId()) // drop object ID
            lew.writeInt(drop.ownerId ?: -1) // owner charid/paryid :)
            lew.write(drop.dropType) // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
            lew.writePos(dropTo!!)
            lew.writeInt((if (drop.dropType.toInt() == 0) drop.ownerId ?: -1 else 0)) //test
            if (mod.toInt() != 2) {
                lew.writePos(dropFrom!!)
                lew.writeShort(0) //Fh?
            }
            if (drop.meso == 0) {
                addExpirationTime(lew, drop.item!!.expiration)
            }
            lew.write(if (drop.playerDrop) 0 else 1) //pet EQP pickup
            return lew.getPacket()
        }

        fun getItemMessage(itemid: Int): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(7)
            lew.writeInt(itemid)
            return lew.getPacket()
        }

        fun getScrollEffect(chr: Int, scrollSuccess: Equip.ScrollResult): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_SCROLL_EFFECT.value)
            lew.writeInt(chr)
            when (scrollSuccess) {
                Equip.ScrollResult.SUCCESS -> {
                    lew.write(1) //v20
                    lew.write(0) //v5
                }
                Equip.ScrollResult.FAIL -> {
                    lew.write(0) //v20
                    lew.write(0) //v5
                }
                Equip.ScrollResult.CURSE -> {
                    lew.write(0) //v20
                    lew.write(1) //v5
                }
            }
            return lew.getPacket()
        }

        fun itemEffect(characterid: Int, itemid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_ITEM_EFFECT.value)
            lew.writeInt(characterid)
            lew.writeInt(itemid)
            return lew.getPacket()
        }

        fun itemExpired(itemid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.write(2)
            lew.writeInt(itemid)
            return lew.getPacket()
        }

        fun showEquipmentLevelUp() = PacketCreator.showSpecialEffect(15)

        fun showItemUnavailable() = CharacterPacket.getShowInventoryStatus(0xfe)

        fun showItemLevelUp() = PacketCreator.showSpecialEffect(15)

        /**
         * Gets a packet telling the client to show an item gain.
         *
         * @param itemId   The ID of the item gained.
         * @param quantity The number of items gained.
         * @param inChat   Show in the chat window?
         * @return The item gain packet.
         */
        fun getShowItemGain(itemId: Int, quantity: Short, inChat: Boolean = false): ByteArray {
            val lew = PacketLittleEndianWriter()
            if (inChat) {
                lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
                lew.write(3)
                lew.write(1)
                lew.writeInt(itemId)
                lew.writeInt(quantity.toInt())
            } else {
                lew.write(SendPacketOpcode.SHOW_STATUS_INFO.value)
                lew.writeShort(0)
                lew.writeInt(itemId)
                lew.writeInt(quantity.toInt())
                lew.writeInt(0)
                lew.writeInt(0)
            }
            return lew.getPacket()
        }
    }
}