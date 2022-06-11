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
            lew.long(PacketCreator.getTime(time))
        }

        fun addRingInfo(plew: PacketLittleEndianWriter, chr: Character) {
            plew.short(chr.crushRings.size)
            for (ring in chr.crushRings) {
                plew.int(ring.partnerId)
                plew.ASCIIString(ring.partnerName, 13)
                plew.int(ring.ringId)
                plew.int(0)
                plew.int(ring.partnerRingId)
                plew.int(0)
            }
            plew.short(chr.friendshipRings.size)
            for (ring in chr.friendshipRings) {
                plew.int(ring.partnerId)
                plew.ASCIIString(ring.partnerName, 13)
                plew.int(ring.ringId)
                plew.int(0)
                plew.int(ring.partnerRingId)
                plew.int(0)
                plew.int(ring.itemId)
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
                        lew.byte(1)
                    }
                    lew.int(ring.ringId)
                    lew.int(0)
                    lew.int(ring.partnerRingId)
                    lew.int(0)
                    lew.int(ring.itemId)
                }
            }
            if (!yes) {
                lew.byte(0)
            }
        }

        fun addPetItemInfo(plew: PacketLittleEndianWriter, item: Item) {
            val pet = item.pet ?: return
            plew.long(0)
            plew.ASCIIString(pet.name, 13)
            plew.byte(pet.level)
            plew.short(pet.closeness)
            plew.byte(pet.fullness)
            addExpirationTime(plew, item.expiration)
            plew.short(0)
            plew.short(-1)
        }

        fun addItemInfo(lew: PacketLittleEndianWriter, item: Item) {
            addItemInfo(lew, item, true, true)
        }

        fun addItemInfo(lew: PacketLittleEndianWriter, item: Item, zeroPosition: Boolean = true, leaveOut: Boolean = true, trade: Boolean = false, chr: Character? = null) {
            val hasUniqueId = ItemInformationProvider.isCash(item.itemId)
            var isRing = false
            var pos = item.position
            if (zeroPosition) {
                if (!leaveOut) lew.byte(0)
            } else {
                if (pos <= -1) {
                    pos = (pos * -1).toByte()
                    if (pos in 101..999) {
                        pos = (pos - 100).toByte()
                    }
                }
                lew.byte(pos)
            }
            lew.byte(if (item.pet != null) 3 else item.getType())
            lew.int(item.itemId)
            lew.bool(hasUniqueId)
            if (hasUniqueId) {
                lew.long(if (item.pet != null) item.petId.toLong() else item.cashId.toLong())
            }
            if (item.pet != null) { // Pet
                addPetItemInfo(lew, item)
                return
            } else {
                addExpirationTime(lew, item.expiration)
                if (item.getType() == 1) {
                    item as Equip
                    isRing = item.ringId > -1
                    lew.byte(item.upgradeSlots)
                    lew.byte(item.level)
                    lew.short(item.str.toInt())
                    lew.short(item.dex.toInt())
                    lew.short(item.int.toInt())
                    lew.short(item.luk.toInt())
                    lew.short(item.hp.toInt())
                    lew.short(item.mp.toInt())
                    lew.short(item.watk.toInt())
                    lew.short(item.matk.toInt())
                    lew.short(item.wdef.toInt())
                    lew.short(item.mdef.toInt())
                    lew.short(item.acc.toInt())
                    lew.short(item.avoid.toInt())
                    lew.short(item.hands.toInt())
                    lew.short(item.speed.toInt())
                    lew.short(item.jump.toInt())
                    lew.gameASCIIString(item.owner)
                    lew.short(item.flag)
                } else {
                    lew.short(item.quantity.toInt())
                    lew.gameASCIIString(item.owner)
                    lew.short(item.flag)
                }
            }
        }

        fun changePetName(chr: Character, newName: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PET_NAMECHANGE.value)
            lew.int(chr.id)
            //lew.byte(0);
            lew.gameASCIIString(newName)
            lew.byte(0)
            return lew.getPacket()
        }

        fun dropItemFromMapObject(drop: MapItem, dropFrom: Point?, dropTo: Point?, mod: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.value)
            lew.byte(mod)
            lew.int(drop.objectId)
            lew.bool(drop.meso > 0) // 1 mesos, 0 item, 2 and above all item meso bag,
            lew.int(drop.getItemId()) // drop object ID
            lew.int(drop.ownerId ?: -1) // owner charid/paryid :)
            lew.byte(drop.dropType) // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
            lew.pos(dropTo!!)
            lew.int((if (drop.dropType.toInt() == 0) drop.ownerId ?: -1 else 0)) //test
            if (mod.toInt() != 2) {
                lew.pos(dropFrom!!)
                lew.short(0) //Fh?
            }
            if (drop.meso == 0) {
                addExpirationTime(lew, drop.item!!.expiration)
            }
            lew.byte(if (drop.playerDrop) 0 else 1) //pet EQP pickup
            return lew.getPacket()
        }

        fun getItemMessage(itemid: Int): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.byte(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.byte(7)
            lew.int(itemid)
            return lew.getPacket()
        }

        fun getScrollEffect(chr: Int, scrollSuccess: Equip.ScrollResult): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_SCROLL_EFFECT.value)
            lew.int(chr)
            when (scrollSuccess) {
                Equip.ScrollResult.SUCCESS -> {
                    lew.byte(1) //v20
                    lew.byte(0) //v5
                }
                Equip.ScrollResult.FAIL -> {
                    lew.byte(0) //v20
                    lew.byte(0) //v5
                }
                Equip.ScrollResult.CURSE -> {
                    lew.byte(0) //v20
                    lew.byte(1) //v5
                }
            }
            return lew.getPacket()
        }

        fun itemEffect(characterid: Int, itemid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_ITEM_EFFECT.value)
            lew.int(characterid)
            lew.int(itemid)
            return lew.getPacket()
        }

        fun itemExpired(itemid: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_STATUS_INFO.value)
            lew.byte(2)
            lew.int(itemid)
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
                lew.byte(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
                lew.byte(3)
                lew.byte(1)
                lew.int(itemId)
                lew.int(quantity.toInt())
            } else {
                lew.byte(SendPacketOpcode.SHOW_STATUS_INFO.value)
                lew.short(0)
                lew.int(itemId)
                lew.int(quantity.toInt())
                lew.int(0)
                lew.int(0)
            }
            return lew.getPacket()
        }
    }
}