package net.server.channel.handlers

import client.Character
import client.Client
import client.inventory.InventoryType
import mu.KLogging
import net.AbstractPacketHandler
import server.CashShop
import server.InventoryManipulator
import server.ItemInformationProvider
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket

class CashOperationHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        val cs = chr.cashShop
        if (cs == null && cs?.opened != true) {
            c.announce(PacketCreator.enableActions())
            return
        }
        when (val action = slea.readByte().toInt()) {
            0x02, 0x1a -> {
                val useNX = if (slea.readByte().toInt() != 0) 2 else 1
                val snCS = slea.readInt()
                val cItem = CashShop.CashItemFactory.getItem(snCS)
                if (cItem == null || !cItem.onSale || cs.getCash(useNX) < cItem.price) return
                if (action == 0x02) { //item
                    val item = cItem.toItem()
                    cs.addToInventory(item)
                    c.announce(CashPacket.showBoughtCashItem(item, c.accountId))
                } else { // Package
                    val cashPackage = CashShop.CashItemFactory.getPackage(cItem.itemId)
                    cashPackage.forEach { cs.addToInventory(it) }
                    c.announce(CashPacket.showBoughtCashPackage(cashPackage, c.accountId))
                }
                cs.gainCash(useNX, -cItem.price)
                c.announce(CashPacket.showCash(chr))
            }
            0x03, 0x1b -> { //TODO check for gender
                val cItem = CashShop.CashItemFactory.getItem(slea.readInt()) ?: return
                val recipient = Character.getCharacterFromDatabase(slea.readGameASCIIString())
                val message = slea.readGameASCIIString()
                if (!canBuy(cItem, cs.getCash(1)) || message.isEmpty() || message.length > 73) {
                    return
                }
                if (recipient == null) {
                    c.announce(CashPacket.showCashShopMessage(0xa9))
                    return
                } else if (recipient.accountId == c.accountId) {
                    c.announce(CashPacket.showCashShopMessage(0xa8))
                    return
                }
                cs.gift(recipient.id, chr.name, message, cItem.sn, -1)
                c.announce(CashPacket.showGiftSucceed(recipient.name, cItem))
                cs.gainCash(1, -cItem.price)
                c.announce(CashPacket.showCash(chr))
                chr.sendNote(recipient.name, "${chr.name} has sent you a gift! Go check out the Cash Shop.", 0)
                c.getChannelServer().players.getCharacterByName(recipient.name)?.showNote()
            }
            0x04 -> { // Modify Wish list
                cs.wishList.clear()
                for (i in 0..9) {
                    val sn = slea.readInt()
                    val cItem = CashShop.CashItemFactory.getItem(sn)
                    if (cItem != null && cItem.onSale && sn != 0) {
                        cs.addToWishList(sn)
                    }
                }
                c.announce(CashPacket.showWishList(chr, true))
            }
            0x05 -> { // Increase Inventory slots
                slea.skip(1)
                val cash = slea.readInt()
                val mode = slea.readByte().toInt()
                if (mode == 0) {
                    val type = slea.readByte()
                    if (cs.getCash(cash)< 4000) return
                    if (chr.gainSlots(type.toInt(), 4, false)) {
                        c.announce(CashPacket.showBoughtInventorySlots(type.toInt(), chr.getSlots(type.toInt())))
                        cs.gainCash(cash, -4000)
                        c.announce(CashPacket.showCash(chr))
                    }
                } else {
                    val cItem = CashShop.CashItemFactory.getItem(slea.readInt()) ?: return
                    val type = (cItem.itemId - 9110000) / 1000
                    if (!canBuy(cItem, cs.getCash(cash))) return
                    if (chr.gainSlots(type, 8, false)) {
                        c.announce(CashPacket.showBoughtInventorySlots(type, chr.getSlots(type)))
                        cs.gainCash(cash, cItem.price)
                        c.announce(CashPacket.showCash(chr))
                    }
                }
            }
            0x06 -> { // Increase storage slots.
                slea.skip(1)
                val cash = slea.readInt()
                val mode = slea.readByte().toInt()
                if (mode == 0) {
                    if (cs.getCash(cash) < 4000) return
                    if (chr.storage?.gainSlots(4) == true) {
                        c.announce(CashPacket.showBoughtStorageSlots(chr.storage.slots.toShort()))
                        cs.gainCash(cash, -4000)
                        c.announce(CashPacket.showCash(chr))
                    }
                } else {
                    val cItem = CashShop.CashItemFactory.getItem(slea.readInt()) ?: return
                    if (!canBuy(cItem, cs.getCash(cash))) return
                    if (chr.storage?.gainSlots(8) == true) {
                        c.announce(CashPacket.showBoughtStorageSlots(chr.storage.slots.toShort()))
                        cs.gainCash(cash, -cItem.price)
                        c.announce(CashPacket.showCash(chr))
                    }
                }
            }
            0x0a -> { // Take from cash inventory
                val item = cs.findByCashId(slea.readInt()) ?: return
                if (chr.getInventory(ItemInformationProvider.getInventoryType(item.itemId))?.addItem(item)?.toInt() != -1) {
                    cs.removeFromInventory(item)
                    c.announce(CashPacket.takeFromCashInventory(item))
                }
            }
            0x0b -> { // Put into cash inventory
                val cashId = slea.readInt()
                slea.skip(4)
                val mi = chr.getInventory(InventoryType.getByType(slea.readByte()))
                val item = mi?.findByCashId(cashId) ?: return
                cs.addToInventory(item)
                mi.removeSlot(item.position)
                c.announce(CashPacket.putIntoCashInventory(item, c.accountId))
            }
            //0x19 -> { // Crush ring
            //0x1f -> // Friendship
            0x1c -> { // 1 meso items
                val itemId = CashShop.CashItemFactory.getItem(slea.readInt())?.itemId
                if (chr.meso.get() > 0) {
                    if (itemId == 4031180 || itemId == 4031192 || itemId == 4031191) {
                        chr.gainMeso(-1, show = false, enableActions = false, inChat = false)
                        InventoryManipulator.addById(c, itemId, 1)
                        c.announce(CashPacket.showBoughtQuestItem(itemId))
                    }
                }
                c.announce(CashPacket.showCash(chr))
            }
            else -> {
                logger.warn { "Unknown cash operation type. $action" }
            }
        }
    }

    companion object : KLogging() {
        fun canBuy(item: CashShop.CashItem?, cash: Int) = item != null && item.onSale && item.price <= cash

        data class SimpleCharacterInfo(var id: Int, var accountId: Int, var name: String)
    }
}