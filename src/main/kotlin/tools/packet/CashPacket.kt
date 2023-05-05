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
import tools.PacketCreator.Companion.packetWriter
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
            lew.long(if (item.petId > -1) item.petId.toLong()
            else if (isRing) equip?.ringId?.toLong() ?: -1
            else item.cashId.toLong())
            if (!isGift) {
                lew.int(accountId)
                lew.int(0)
            }
            lew.int(item.itemId)
            if (!isGift) {
                lew.int(item.sn)
                lew.short(item.quantity.toInt())
            }
            lew.ASCIIString(item.giftFrom, 13)
            if (isGift) {
                giftMessage?.let { lew.ASCIIString(it, 73) }
                return
            }
            ItemPacket.addExpirationTime(lew, item.expiration)
            lew.long(0)
        }

        fun addPetInfo(lew: PacketLittleEndianWriter, pet: Pet?) {
            if (pet == null) return
            lew.byte(1)
            lew.int(pet.itemId)
            lew.gameASCIIString(pet.name)
            lew.int(pet.uniqueId)
            lew.int(0)
            lew.pos(pet.pos ?: Point(0, 0))
            lew.byte(pet.stance)
            lew.int(pet.fh)
        }

        fun addTeleportInfo(lew: PacketLittleEndianWriter, chr: Character) {
            chr.trockMaps.forEach { lew.int(it) }
            chr.vipTrockMaps.forEach { lew.int(it) }
        }

        fun destroyHiredMerchant(id: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.DESTROY_HIRED_MERCHANT.value)
            lew.int(id)
            return lew.getPacket()
        }

        fun fredrickMessage(operation: Byte): ByteArray {
            val plew = PacketLittleEndianWriter()
            plew.byte(SendPacketOpcode.FREDRICK_MESSAGE.value)
            plew.byte(operation)
            return plew.getPacket()
        }

        fun getFredrick(chr: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.FREDRICK.value)
            lew.byte(0x23)
            lew.int(9030000) // Fredrick
            lew.int(32272) //id
            lew.skip(5)
            lew.int(chr?.merchantMeso ?: 0)
            lew.byte(0)
            val items = ItemFactory.MERCHANT.loadItems(chr?.id ?: -1, false)
            lew.byte(items.size)
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
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.ROOM.code)
            lew.byte(0x05)
            lew.byte(0x04)
            lew.short(hm.getVisitorSlot(chr) + 1)
            lew.int(hm.itemId)
            lew.gameASCIIString("고용상인")
            for (i in 0..2) {
                val v = hm.visitors[i]
                if (v != null) {
                    lew.byte(i + 1)
                    CharacterPacket.addCharLook(lew, v, false)
                    lew.gameASCIIString(v.name)
                }
            }
            lew.byte(-1)
            if (hm.isOwner(chr)) {
                lew.short(hm.messages.size)
                for (i in hm.messages.indices) {
                    lew.gameASCIIString(hm.messages[i].first)
                    lew.byte(hm.messages[i].second)
                }
            } else {
                lew.short(0)
            }
            lew.gameASCIIString(hm.ownerName)
            if (hm.isOwner(chr)) {
                lew.int(hm.getTimeLeft())
                lew.byte(if (firstTime) 1 else 0)
                //List<SoldItem> sold = hm.getSold();
                lew.byte(0) //sold.size()
                /*for (SoldItem s : sold) { fix this
             lew.int(s.getItemId());
             lew.short(s.getQuantity());
             lew.int(s.getMesos());
             lew.writeAsciiString(s.getBuyer());
             }*/lew.int(chr.merchantMeso) //:D?
            }
            lew.gameASCIIString(hm.description)
            lew.byte(0x10) //SLOTS, which is 16 for most stores...slotMax
            lew.int(chr.meso.get())
            lew.byte(hm.items.size)
            if (hm.items.isEmpty()) {
                lew.byte(0) //Hmm??
            } else {
                for ((item1, bundles, price) in hm.items) {
                    lew.short(bundles.toInt())
                    lew.short(item1.quantity.toInt())
                    lew.int(price)
                    ItemPacket.addItemInfo(lew, item1, zeroPosition = true, leaveOut = true)
                }
            }
            return lew.getPacket()
        }

        fun hiredMerchantBox(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.ENTRUSTED_SHOP_CHECK_RESULT.value) // header.
            lew.byte(0x06)
            return lew.getPacket()
        }

        fun hiredMerchantChat(message: String, slot: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.CHAT.code)
            lew.byte(PlayerInteractionHandler.Action.CHAT_THING.code)
            lew.byte(slot)
            lew.gameASCIIString(message)
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
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.REAL_CLOSE_MERCHANT.code)
            lew.byte(0)
            return lew.getPacket()
        }

        fun hiredMerchantVisitorAdd(chr: Character, slot: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.VISIT.code)
            lew.byte(slot)
            CharacterPacket.addCharLook(lew, chr, false)
            lew.gameASCIIString(chr.name)
            return lew.getPacket()
        }

        fun hiredMerchantVisitorLeave(slot: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.EXIT.code)
            if (slot != 0) {
                lew.byte(slot)
            }
            return lew.getPacket()
        }

        fun enableCSUse() = packetWriter(SendPacketOpcode.CS_USE) {
            byte(0)
            int(0)
        }

        fun leaveHiredMerchant(slot: Int, status2: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.EXIT.code)
            lew.byte(slot)
            lew.byte(status2)
            return lew.getPacket()
        }

        @Throws(Exception::class)
        fun openCashShop(c: Client): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SET_CASH_SHOP.value)
            c.player?.let { CharacterPacket.addCharacterInfo( lew, it) }
            lew.gameASCIIString("nxId") // NX ID
            lew.skip(123)
            for (i in 1..8) { //TODO: check this id. might be recommend items?
                for (j in 0..1) {
                    lew.int(i)
                    lew.int(j)
                    lew.int(10000804)
                    lew.int(i)
                    lew.int(j)
                    lew.int(10000383)
                    lew.int(i)
                    lew.int(j)
                    lew.int(10000872)
                    lew.int(i)
                    lew.int(j)
                    lew.int(10000731)
                    lew.int(i)
                    lew.int(j)
                    lew.int(10000834)
                }
            }
            lew.short(0)
            lew.long(0)
            lew.long(0)
            lew.long(0)
            return lew.getPacket()
        }

        fun petChat(cid: Int, index: Byte, act: Int, text: String): ByteArray {
            val plew = PacketLittleEndianWriter()
            plew.byte(SendPacketOpcode.PET_CHAT.value)
            plew.int(cid)
            //plew.byte(index);
            plew.byte(0)
            plew.byte(act)
            plew.gameASCIIString(text)
            return plew.getPacket()
        }

        fun petCommandResponse(cid: Int, index: Byte, animation: Int, success: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.PET_COMMAND.value)
            lew.int(cid)
            lew.byte(if (animation == 1 || !success) 1 else 0)
            lew.byte(animation)
            if (animation == 1) {
                lew.byte(0)
            } else {
                lew.byte(if (success) 1 else 0)
            }
            return lew.getPacket()
        }

        fun petStatUpdate(chr: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.STAT_CHANGED.value)
            var mask = 0
            mask = mask or CharacterStat.PET.value
            lew.byte(0)
            lew.int(mask)
            val pet = chr?.pet
            lew.long(pet?.uniqueId?.toLong() ?: -1)
            lew.byte(0) //stat changed
            return lew.getPacket()
        }

        fun putIntoCashInventory(item: Item, accountId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(55)
            addCashItemInformation(lew, item, accountId)
            return lew.getPacket()
        }

        fun sendPetAutoHpPot(itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.AUTO_HP_POT.value)
            lew.int(itemId)
            return lew.getPacket()
        }

        fun sendPetAutoMpPot(itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.byte(SendPacketOpcode.AUTO_MP_POT.value)
            lew.int(itemId)
            return lew.getPacket()
        }

        fun showBoughtCashItem(item: Item, accountId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(40)
            addCashItemInformation(lew, item, accountId)
            return lew.getPacket()
        }


        fun showBoughtCashPackage(cashPackage: List<Item>, accountId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(84)
            lew.byte(cashPackage.size)
            for (item in cashPackage) {
                addCashItemInformation(lew, item, accountId)
            }
            lew.short(0)
            return lew.getPacket()
        }

        fun showBoughtInventorySlots(type: Int, slots: Byte?): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(49)
            lew.byte(type)
            lew.short(slots?.toInt() ?: -1)
            return lew.getPacket()
        }

        fun showBoughtQuestItem(itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(0x8D)
            lew.int(1)
            lew.short(1)
            lew.byte(0x0B)
            lew.byte(0)
            lew.int(itemId)
            return lew.getPacket()
        }

        fun showBoughtStorageSlots(slots: Short): ByteArray {
            val lew = PacketLittleEndianWriter(5)
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(51)
            lew.short(slots.toInt())
            return lew.getPacket()
        }

        fun showCash(mc: Character): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.QUERY_CASH_RESULT.value)
            lew.int(mc.cashShop?.getCash(1) ?: 0)
            lew.int(mc.cashShop?.getCash(2) ?: 0)
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
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(41)
            lew.byte(message)
            return lew.getPacket()
        }

        fun showCouponRedeemedItem(itemId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.short(0x49) //v72
            lew.int(0)
            lew.int(1)
            lew.short(1)
            lew.short(0x1A)
            lew.int(itemId)
            lew.int(0)
            return lew.getPacket()
        }

        fun showGiftSucceed(to: String, item: CashShop.CashItem): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(47) //0x5D, Couldn't be sent
            lew.gameASCIIString(to)
            lew.int(item.itemId)
            lew.short(item.count.toInt())
            lew.int(item.price)
            return lew.getPacket()
        }

        fun showLocker(c: Client): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(34)
            val cs = c.player?.cashShop ?: return ByteArray(0)
            lew.short(cs.inventory.size)
            for (item in cs.inventory) {
                addCashItemInformation(lew, item, c.accountId)
            }
            val gifts = cs.loadGifts()
            lew.short(gifts.size)
            gifts.forEach { (first, second) ->
                addCashItemInformation(lew, first, 0, second)
            }
            lew.short(c.player?.storage?.slots?.toInt() ?: 0)
            return lew.getPacket()
        }

        fun showOwnPetLevelUp(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.byte(4)
            lew.byte(0)
            lew.byte(0) // Pet Index
            return lew.getPacket()
        }

        fun showPet(chr: Character, pet: Pet?, remove: Boolean, hunger: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SPAWN_PET.value)
            lew.int(chr.id)
            if (remove) {
                lew.short(if (hunger) 0x100 else 0)
            } else {
                addPetInfo(lew, pet)
            }
            return lew.getPacket()
        }

        fun showPetLevelUp(chr: Character?): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.int(chr?.id ?: -1)
            lew.byte(4)
            lew.byte(0)
            lew.byte(0) // pet index
            return lew.getPacket()
        }

        fun showWishList(mc: Character, update: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            if (update) {
                lew.byte(38)
            } else {
                lew.byte(36)
            }
            mc.cashShop?.let { cashShop ->
                for (sn in cashShop.wishList) {
                    lew.int(sn)
                }
                for (i in cashShop.wishList.size..9) {
                    lew.int(0)
                }
            }
            return lew.getPacket()
        }

        fun spawnHiredMerchant(hm: HiredMerchant): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SPAWN_HIRED_MERCHANT.value)
            lew.int(hm.ownerId)
            lew.int(hm.itemId)
            lew.short(hm.position.x)
            lew.short(hm.position.y)
            lew.short(0)
            lew.gameASCIIString(hm.ownerName)
            lew.byte(0x05)
            lew.int(hm.objectId)
            lew.gameASCIIString(hm.description)
            lew.byte(hm.itemId % 10)
            lew.byte(byteArrayOf(1, 4))
            return lew.getPacket()
        }

        fun takeFromCashInventory(item: Item): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.CASHSHOP_OPERATION.value)
            lew.byte(53)
            lew.short(item.position.toInt())
            ItemPacket.addItemInfo(lew, item, zeroPosition = true, leaveOut = true)
            return lew.getPacket()
        }

        fun trockRefreshMapList(chr: Character, delete: Boolean, vip: Boolean): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.MAP_TRANSFER_RESULT.value)
            lew.byte(if (delete) 2 else 3)
            if (vip) {
                lew.byte(1)
                val map = chr.vipTrockMaps
                for (i in 0..9) {
                    lew.int(map[i])
                }
            } else {
                lew.byte(0)
                val map = chr.trockMaps
                for (i in 0..4) {
                    lew.int(map[i])
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
            lew.byte(SendPacketOpcode.PLAYER_INTERACTION.value)
            lew.byte(PlayerInteractionHandler.Action.UPDATE_MERCHANT.code)
            lew.int(chr?.meso?.get() ?: 0)
            lew.byte(hm.items.size)
            for ((item1, bundles, price) in hm.items) {
                lew.short(bundles.toInt())
                lew.short(item1.quantity.toInt())
                lew.int(price)
                ItemPacket.addItemInfo(lew, item1, zeroPosition = true, leaveOut = true)
            }
            return lew.getPacket()
        }
    }
}