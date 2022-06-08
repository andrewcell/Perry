package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import constants.ItemConstants
import net.AbstractPacketHandler
import server.InventoryManipulator
import server.ItemInformationProvider
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class StorageHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        val mode = slea.readByte().toInt()
        val storage = chr.storage
        when (mode) {
            4 -> { //take out
                val type = slea.readByte()
                val slot = slea.readByte()
                val storageSlot = storage?.getSlot(InventoryType.getByType(type), slot) ?: return
                var item = storage.getItem(storageSlot)
                if (ItemInformationProvider.isPickupRestricted(item.itemId) && chr.getItemQuantity(item.itemId, true) > 0) {
                    c.announce(CharacterPacket.getStorageError(0x0c))
                }
                if (chr.map.mapId == 910000000) {
                    if (chr.meso.get() < 1000) {
                        c.announce(CharacterPacket.getStorageError(0x0b))
                        return
                    } else chr.gainMeso(-1000, false)
                }
                if (InventoryManipulator.checkSpace(c, item.itemId, item.quantity.toInt(), item.owner)) {
                    item = storage.takeOut(storageSlot)
                    if ((item.flag and ItemConstants.KARMA) == ItemConstants.KARMA) {
                        item.flag = item.flag xor ItemConstants.KARMA
                    } else if (item.getType() == 2 && (item.flag and ItemConstants.SPIKES) == ItemConstants.SPIKES)
                        item.flag = item.flag xor ItemConstants.SPIKES
                    InventoryManipulator.addFromDrop(c, item, false)
                    storage.sendTakenOut(c, ItemInformationProvider.getInventoryType(item.itemId))
                } else c.announce(CharacterPacket.getStorageError(0x0a))
            }
            5 -> { // store
                val slot = slea.readShort().toByte()
                val itemId = slea.readInt()
                var quantity = slea.readShort()
                if (quantity < 1 || chr.getItemQuantity(itemId, false) < quantity) return
                if (storage?.isFull() == true) {
                    c.announce(CharacterPacket.getStorageError(0x11))
                    return
                }
                val meso = if (chr.map.mapId == 910000000) -500 else - 100
                if (chr.meso.get() < meso) {
                    c.announce(CharacterPacket.getStorageError(0x0b))
                } else {
                    val type = ItemInformationProvider.getInventoryType(itemId)
                    val item = chr.getInventory(type)?.getItem(slot)?.copy() ?: return
                    if (item.itemId == itemId && (item.quantity >= quantity || ItemConstants.isRechargeable(itemId))) {
                        if (ItemConstants.isRechargeable(itemId)) quantity = item.quantity
                        chr.gainMeso(meso, show = false, enableActions = true, inChat = false)
                        InventoryManipulator.removeFromSlot(c, type, slot, quantity, fromDrop = false, consume = false)
                        item.quantity = quantity
                        storage?.store(item)
                        storage?.sendStored(c, ItemInformationProvider.getInventoryType(itemId))
                    }
                }
            }
            7 -> { // meso
                var meso = slea.readInt()
                val storageMesos = storage?.meso ?: 0
                val playerMesos = chr.meso.get()
                if ((meso in 1..storageMesos) || (meso < 0 && playerMesos >= -meso)) {
                    if (meso < 0 && (storageMesos - meso) < 0) {
                        meso = -2147483648 + storageMesos
                        if (meso < playerMesos) return
                    } else if (meso > 0 && (playerMesos + meso) < 0) {
                        meso = 2147483647 - playerMesos
                        if (meso > storageMesos) return
                    }
                    storage?.meso = storageMesos - meso
                    chr.gainMeso(meso, show = false, enableActions = true, inChat = false)
                } else return
                storage?.sendMeso(c)
            }
            8 -> storage?.close() // close
        }
    }
}