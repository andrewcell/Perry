package tools.packet

import client.Character
import client.inventory.Equip
import client.inventory.Item
import net.SendPacketOpcode
import server.ItemInformationProvider
import server.maps.MapItem
import tools.PacketCreator
import tools.PacketCreator.Companion.packetWriter
import tools.data.output.PacketLittleEndianWriter
import java.awt.Point

//Packet for item
class ItemPacket {
    companion object {
        fun addExpirationTime(time: Long) = packetWriter {
            long(PacketCreator.getTime(time))
        }

        fun addRingInfo(chr: Character) = packetWriter {
            short(chr.crushRings.size)
            for (ring in chr.crushRings) {
                int(ring.partnerId)
                ASCIIString(ring.partnerName, 13)
                int(ring.ringId)
                int(0)
                int(ring.partnerRingId)
                int(0)
            }
            short(chr.friendshipRings.size)
            for (ring in chr.friendshipRings) {
                int(ring.partnerId)
                ASCIIString(ring.partnerName, 13)
                int(ring.ringId)
                int(0)
                int(ring.partnerRingId)
                int(0)
                int(ring.itemId)
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

        fun addPetItemInfo(item: Item) = packetWriter {
            val pet = item.pet ?: return@packetWriter
            long(0)
            ASCIIString(pet.name, 13)
            byte(pet.level)
            short(pet.closeness)
            byte(pet.fullness)
            addExpirationTime(item.expiration)
            short(0)
            short(-1)
        }

        fun addItemInfo(item: Item) = addItemInfo(item, true, true)

        fun addItemInfo(item: Item, zeroPosition: Boolean = true, leaveOut: Boolean = true, trade: Boolean = false, chr: Character? = null) = packetWriter {
            val hasUniqueId = ItemInformationProvider.isCash(item.itemId)
            var isRing = false
            var pos = item.position
            if (zeroPosition) {
                if (!leaveOut) byte(0)
            } else {
                if (pos <= -1) {
                    pos = (pos * -1).toByte()
                    if (pos in 101..999) {
                        pos = (pos - 100).toByte()
                    }
                }
                byte(pos)
            }
            byte(if (item.pet != null) 3 else item.getType())
            int(item.itemId)
            bool(hasUniqueId)
            if (hasUniqueId) {
                long(if (item.pet != null) item.petId.toLong() else item.cashId.toLong())
            }
            if (item.pet != null) { // Pet
                addPetItemInfo(item)
                return@packetWriter
            } else {
                addExpirationTime(item.expiration)
                if (item.getType() == 1) {
                    item as Equip
                    isRing = item.ringId > -1
                    byte(item.upgradeSlots)
                    byte(item.level)
                    short(item.str.toInt())
                    short(item.dex.toInt())
                    short(item.int.toInt())
                    short(item.luk.toInt())
                    short(item.hp.toInt())
                    short(item.mp.toInt())
                    short(item.watk.toInt())
                    short(item.matk.toInt())
                    short(item.wdef.toInt())
                    short(item.mdef.toInt())
                    short(item.acc.toInt())
                    short(item.avoid.toInt())
                    short(item.hands.toInt())
                    short(item.speed.toInt())
                    short(item.jump.toInt())
                    gameASCIIString(item.owner)
                    short(item.flag)
                } else {
                    short(item.quantity.toInt())
                    gameASCIIString(item.owner)
                    short(item.flag)
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
                addExpirationTime(drop.item!!.expiration)
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