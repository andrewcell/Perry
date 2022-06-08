package tools.packet

import client.Character
import client.CharacterStat
import client.Client
import client.inventory.Equip
import client.inventory.Item
import client.inventory.ItemFactory
import client.inventory.Pet
import net.SendPacketOpcode
import net.server.channel.handlers.PlayerInteractionHandler
import server.CashShop
import server.maps.HiredMerchant
import tools.data.output.PacketLittleEndianWriter
import java.awt.Point

// Packet for Cash shop, cash item, pet, hired merchant...
class CashPacket {
    companion object {
        fun addCashItemInformation(lew: PacketLittleEndianWriter, item: Item, accountId: Int, giftMessage: String? = null) {
            val isGift = giftMessage != null
            var isRing = false
            var equip: Equip? = null
            if (item.getType() == 1) {
                equip = item as Equip
                isRing = equip.ringId > -1
            }
            lew.writeLong(if (item.petId > -1) item.petId.toLong()
            else if (isRing) equip?.ringId?.toLong() ?: -1
            else item.cashId.toLong())
            if (!isGift) {
                lew.writeInt(accountId)
                lew.writeInt(0)
            }
            lew.writeInt(item.itemId)
            if (!isGift) {
                lew.writeInt(item.sn)
                lew.writeShort(item.quantity.toInt())
            }
            lew.writeASCIIString(item.giftFrom, 13)
            if (isGift) {
                giftMessage?.let { lew.writeASCIIString(it, 73) }
                return
            }
            ItemPacket.addExpirationTime(lew, item.expiration)
            lew.writeLong(0)
        }

        fun addPetInfo(lew: PacketLittleEndianWriter, pet: Pet?) {
            if (pet == null) return
            lew.write(1)
            lew.writeInt(pet.itemId)
            lew.writeGameASCIIString(pet.name)
            lew.writeInt(pet.uniqueId)
            lew.writeInt(0)
            lew.writePos(pet.pos ?: Point(0, 0))
            lew.write(pet.stance)
            lew.writeInt(pet.fh)
        }

        fun addTeleportInfo(lew: PacketLittleEndianWriter, chr: Character) {
            chr.trockMaps.forEach { lew.writeInt(it) }
            chr.vipTrockMaps.forEach { lew.writeInt(it) }
        }

        fun destroyHiredMerchant(id: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.DESTROY_HIRED_MERCHANT.value)
            lew.writeInt(id)
            return lew.getPacket()
        }

        fun fredrickMessage(operation: Byte): ByteArray {
            val plew = PacketLittleEndianWriter()
            plew.write(SendPacketOpcode.FREDRICK_MESSAGE.value)
            plew.write(operation)
            return plew.getPacket()
        }

        fun getFredrick(chr: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.FREDRICK.value)
            lew.write(0x23)
            lew.writeInt(9030000) // Fredrick
            lew.writeInt(32272) //id
            lew.skip(5)
            lew.writeInt(chr?.merchantMeso ?: 0)
            lew.write(0)
            val items = ItemFactory.MERCHANT.loadItems(chr?.id ?: -1, false)
            lew.write(items.size)
            for (i in items.indices) {
                ItemPacket.addItemInfo(lew, items[i].first, zeroPosition = true, leaveOut = true)
            }
            lew.skip(3)
            return lew.getPacket()
        }

        /**
         * Possible things for ENTRUSTED_SHOP_CHECK_RESULT
         * 0x0E = 00 = Renaming Failed - Can't find the merchant, 01 = Renaming succesful
         * 0x10 = Changes channel to the store (Store is open at Channel 1, do you want to change channels?)
         * 0x11 = You cannot sell any items when managing.. blabla
         * 0x12 = FKING POPUP LOL
        */
        fun getHiredMerchant(chr: Character, hm: HiredMerchant, firstTime: Boolean): ByteArray { //Thanks Dustin
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.ROOM.code)
            lew.write(0x05)
            lew.write(0x04)
            lew.writeShort(hm.getVisitorSlot(chr) + 1)
            lew.writeInt(hm.itemId)
            lew.writeGameASCIIString("고용상인")
            for (i in 0..2) {
                val v = hm.visitors[i]
                if (v != null) {
                    lew.write(i + 1)
                    CharacterPacket.addCharLook(lew, v, false)
                    lew.writeGameASCIIString(v.name)
                }
            }
            lew.write(-1)
            if (hm.isOwner(chr)) {
                lew.writeShort(hm.messages.size)
                for (i in hm.messages.indices) {
                    lew.writeGameASCIIString(hm.messages[i].first)
                    lew.write(hm.messages[i].second)
                }
            } else {
                lew.writeShort(0)
            }
            lew.writeGameASCIIString(hm.ownerName)
            if (hm.isOwner(chr)) {
                lew.writeInt(hm.getTimeLeft())
                lew.write(if (firstTime) 1 else 0)
                //List<SoldItem> sold = hm.getSold();
                lew.write(0) //sold.size()
                /*for (SoldItem s : sold) { fix this
             lew.writeInt(s.getItemId());
             lew.writeShort(s.getQuantity());
             lew.writeInt(s.getMesos());
             lew.writeAsciiString(s.getBuyer());
             }*/lew.writeInt(chr.merchantMeso) //:D?
            }
            lew.writeGameASCIIString(hm.description)
            lew.write(0x10) //SLOTS, which is 16 for most stores...slotMax
            lew.writeInt(chr.meso.get())
            lew.write(hm.items.size)
            if (hm.items.isEmpty()) {
                lew.write(0) //Hmm??
            } else {
                for ((item1, bundles, price) in hm.items) {
                    lew.writeShort(bundles.toInt())
                    lew.writeShort(item1.quantity.toInt())
                    lew.writeInt(price)
                    ItemPacket.addItemInfo(lew, item1, zeroPosition = true, leaveOut = true)
                }
            }
            return lew.getPacket()
        }

        fun hiredMerchantBox(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.ENTRUSTED_SHOP_CHECK_RESULT.value) // header.
            lew.write(0x06)
            return lew.getPacket()
        }

        fun hiredMerchantChat(message: String, slot: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.CHAT.code)
            lew.write(PlayerInteractionHandler.Action.CHAT_THING.code)
            lew.write(slot)
            lew.writeGameASCIIString(message)
            return lew.getPacket()
        }

        /*
         * Possible things for ENTRUSTED_SHOP_CHECK_RESULT
         * 0x0E = 00 = Renaming Failed - Can't find the merchant, 01 = Renaming succesful
         * 0x10 = Changes channel to the store (Store is open at Channel 1, do you want to change channels?)
         * 0x11 = You cannot sell any items when managing.. blabla
         * 0x12 = POPUP
         */
        fun hiredMerchantOwnerLeave(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.REAL_CLOSE_MERCHANT.code)
            lew.write(0)
            return lew.getPacket()
        }

        fun hiredMerchantVisitorAdd(chr: Character, slot: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.VISIT.code)
            lew.write(slot)
            CharacterPacket.addCharLook(lew, chr, false)
            lew.writeGameASCIIString(chr.name)
            return lew.getPacket()
        }

        fun hiredMerchantVisitorLeave(slot: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.EXIT.code)
            if (slot != 0) {
                lew.write(slot)
            }
            return lew.getPacket()
        }

        fun enableCSUse(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CS_USE.value)
            lew.write(0)
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun leaveHiredMerchant(slot: Int, status2: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.EXIT.code)
            lew.write(slot)
            lew.write(status2)
            return lew.getPacket()
        }

        @Throws(Exception::class)
        fun openCashShop(c: Client): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SET_CASH_SHOP.value)
            c.player?.let { CharacterPacket.addCharacterInfo(lew, it) }
            lew.writeGameASCIIString("nxId") // NX ID
            lew.skip(123)
            for (i in 1..8) { //TODO: check this id. might be recommend items?
                for (j in 0..1) {
                    lew.writeInt(i)
                    lew.writeInt(j)
                    lew.writeInt(10000804)
                    lew.writeInt(i)
                    lew.writeInt(j)
                    lew.writeInt(10000383)
                    lew.writeInt(i)
                    lew.writeInt(j)
                    lew.writeInt(10000872)
                    lew.writeInt(i)
                    lew.writeInt(j)
                    lew.writeInt(10000731)
                    lew.writeInt(i)
                    lew.writeInt(j)
                    lew.writeInt(10000834)
                }
            }
            lew.writeShort(0)
            lew.writeLong(0)
            lew.writeLong(0)
            lew.writeLong(0)
            return lew.getPacket()
        }

        fun petChat(cid: Int, index: Byte, act: Int, text: String): ByteArray {
            val plew = PacketLittleEndianWriter()
            plew.write(SendPacketOpcode.PET_CHAT.value)
            plew.writeInt(cid)
            //plew.write(index);
            plew.write(0)
            plew.write(act)
            plew.writeGameASCIIString(text)
            return plew.getPacket()
        }

        fun petCommandResponse(cid: Int, index: Byte, animation: Int, success: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PET_COMMAND.value)
            lew.writeInt(cid)
            lew.write(if (animation == 1 || !success) 1 else 0)
            lew.write(animation)
            if (animation == 1) {
                lew.write(0)
            } else {
                lew.write(if (success) 1 else 0)
            }
            return lew.getPacket()
        }

        fun petStatUpdate(chr: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.STAT_CHANGED.value)
            var mask = 0
            mask = mask or CharacterStat.PET.value
            lew.write(0)
            lew.writeInt(mask)
            val pet = chr?.pet
            lew.writeLong(pet?.uniqueId?.toLong() ?: -1)
            lew.write(0) //stat changed
            return lew.getPacket()
        }

        fun putIntoCashInventory(item: Item, accountId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(55)
            addCashItemInformation(lew, item, accountId)
            return lew.getPacket()
        }

        fun sendPetAutoHpPot(itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.AUTO_HP_POT.value)
            lew.writeInt(itemId)
            return lew.getPacket()
        }

        fun sendPetAutoMpPot(itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.write(SendPacketOpcode.AUTO_MP_POT.value)
            lew.writeInt(itemId)
            return lew.getPacket()
        }

        fun showBoughtCashItem(item: Item, accountId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(40)
            addCashItemInformation(lew, item, accountId)
            return lew.getPacket()
        }


        fun showBoughtCashPackage(cashPackage: List<Item>, accountId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(84)
            lew.write(cashPackage.size)
            for (item in cashPackage) {
                addCashItemInformation(lew, item, accountId)
            }
            lew.writeShort(0)
            return lew.getPacket()
        }

        fun showBoughtInventorySlots(type: Int, slots: Byte?): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(49)
            lew.write(type)
            lew.writeShort(slots?.toInt() ?: -1)
            return lew.getPacket()
        }

        fun showBoughtQuestItem(itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(0x8D)
            lew.writeInt(1)
            lew.writeShort(1)
            lew.write(0x0B)
            lew.write(0)
            lew.writeInt(itemId)
            return lew.getPacket()
        }

        fun showBoughtStorageSlots(slots: Short): ByteArray {
            val lew = PacketLittleEndianWriter(5)
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(51)
            lew.writeShort(slots.toInt())
            return lew.getPacket()
        }

        fun showCash(mc: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.QUERY_CASH_RESULT.value)
            lew.writeInt(mc.cashShop?.getCash(1) ?: 0)
            lew.writeInt(mc.cashShop?.getCash(2) ?: 0)
            return lew.getPacket()
        }

        /*
         * 00 = Due to an unknown error, failed
         * A4 = Due to an unknown error, failed + warpout
         * A5 = You don't have enough cash.
         * A6 = long as shet msg
         * A7 = You have exceeded the allotted limit of price for gifts.
         * A8 = You cannot send a gift to your own account. Log in on the char and purchase
         * A9 = Please confirm whether the character's name is correct.
         * AA = Gender restriction!
         * //Skipped a few
         * B0 = Wrong Coupon Code
         * B1 = Disconnect from CS because of 3 wrong coupon codes < lol
         * B2 = Expired Coupon
         * B3 = Coupon has been used already
         * B4 = NX internet cafes?
         *
         * BB = inv full
         * C2 = not enough mesos? Lol not even 1 mesos xD
         */
        fun showCashShopMessage(message: Int): ByteArray {
            val lew = PacketLittleEndianWriter(4)
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(41)
            lew.write(message)
            return lew.getPacket()
        }

        fun showCouponRedeemedItem(itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.writeShort(0x49) //v72
            lew.writeInt(0)
            lew.writeInt(1)
            lew.writeShort(1)
            lew.writeShort(0x1A)
            lew.writeInt(itemId)
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun showGiftSucceed(to: String, item: CashShop.CashItem): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(47) //0x5D, Couldn't be sent
            lew.writeGameASCIIString(to)
            lew.writeInt(item.itemId)
            lew.writeShort(item.count.toInt())
            lew.writeInt(item.price)
            return lew.getPacket()
        }

        fun showLocker(c: Client): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(34)
            val cs = c.player?.cashShop ?: return ByteArray(0)
            lew.writeShort(cs.inventory.size)
            for (item in cs.inventory) {
                addCashItemInformation(lew, item, c.accountId)
            }
            val gifts = cs.loadGifts()
            lew.writeShort(gifts.size)
            gifts.forEach { (first, second) ->
                addCashItemInformation(lew, first, 0, second)
            }
            lew.writeShort(c.player?.storage?.slots?.toInt() ?: 0)
            return lew.getPacket()
        }

        fun showOwnPetLevelUp(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.write(4)
            lew.write(0)
            lew.write(0) // Pet Index
            return lew.getPacket()
        }

        fun showPet(chr: Character, pet: Pet?, remove: Boolean, hunger: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_PET.value)
            lew.writeInt(chr.id)
            if (remove) {
                lew.writeShort(if (hunger) 0x100 else 0)
            } else {
                addPetInfo(lew, pet)
            }
            return lew.getPacket()
        }

        fun showPetLevelUp(chr: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.writeInt(chr?.id ?: -1)
            lew.write(4)
            lew.write(0)
            lew.write(0) // pet index
            return lew.getPacket()
        }

        fun showWishList(mc: Character, update: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            if (update) {
                lew.write(38)
            } else {
                lew.write(36)
            }
            mc.cashShop?.let { cashShop ->
                for (sn in cashShop.wishList) {
                    lew.writeInt(sn)
                }
                for (i in cashShop.wishList.size..9) {
                    lew.writeInt(0)
                }
            }
            return lew.getPacket()
        }

        fun spawnHiredMerchant(hm: HiredMerchant): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_HIRED_MERCHANT.value)
            lew.writeInt(hm.ownerId)
            lew.writeInt(hm.itemId)
            lew.writeShort(hm.position.x)
            lew.writeShort(hm.position.y)
            lew.writeShort(0)
            lew.writeGameASCIIString(hm.ownerName)
            lew.write(0x05)
            lew.writeInt(hm.objectId)
            lew.writeGameASCIIString(hm.description)
            lew.write(hm.itemId % 10)
            lew.write(byteArrayOf(1, 4))
            return lew.getPacket()
        }

        fun takeFromCashInventory(item: Item): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.write(53)
            lew.writeShort(item.position.toInt())
            ItemPacket.addItemInfo(lew, item, zeroPosition = true, leaveOut = true)
            return lew.getPacket()
        }

        fun trockRefreshMapList(chr: Character, delete: Boolean, vip: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.MAP_TRANSFER_RESULT.value)
            lew.write(if (delete) 2 else 3)
            if (vip) {
                lew.write(1)
                val map = chr.vipTrockMaps
                for (i in 0..9) {
                    lew.writeInt(map[i])
                }
            } else {
                lew.write(0)
                val map = chr.trockMaps
                for (i in 0..4) {
                    lew.writeInt(map[i])
                }
            }
            return lew.getPacket()
        }

        /*
         * Possible things for ENTRUSTED_SHOP_CHECK_RESULT
         * 0x0E = 00 = Renaming Failed - Can't find the merchant, 01 = Renaming succesful
         * 0x10 = Changes channel to the store (Store is open at Channel 1, do you want to change channels?)
         * 0x11 = You cannot sell any items when managing.. blabla
         * 0x12 = FKING POPUP LOL
         */
        fun updateHiredMerchant(hm: HiredMerchant, chr: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.write(PlayerInteractionHandler.Action.UPDATE_MERCHANT.code)
            lew.writeInt(chr?.meso?.get() ?: 0)
            lew.write(hm.items.size)
            for ((item1, bundles, price) in hm.items) {
                lew.writeShort(bundles.toInt())
                lew.writeShort(item1.quantity.toInt())
                lew.writeInt(price)
                ItemPacket.addItemInfo(lew, item1, zeroPosition = true, leaveOut = true)
            }
            return lew.getPacket()
        }
    }
}