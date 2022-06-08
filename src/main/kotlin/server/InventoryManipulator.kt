package server

import client.BuffStat
import client.Client
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.ModifyInventory
import constants.ItemConstants
import mu.KLogging
import tools.PacketCreator
import tools.packet.CharacterPacket
import tools.packet.ItemPacket
import java.awt.Point
import kotlin.math.ceil
import server.ItemInformationProvider as ii

class InventoryManipulator {
    companion object : KLogging() {
        fun addById(c: Client, itemId: Int, quantity: Short) = addById(c, itemId, quantity, null, -1, -1)

        fun addById(c: Client, itemId: Int, quantity: Short, expiration: Long) = addById(c, itemId, quantity, null, -1, 0, expiration)

        fun addById(c: Client, itemId: Int, quantity: Short, owner: String?, petId: Int) = addById(c, itemId, quantity, owner, petId, -1)

        fun addById(c: Client, itemId: Int, quantity: Short, owner: String?, petId: Int, expiration: Long) = addById(c, itemId, quantity, owner, petId, 0, expiration)

        fun addById(c: Client, itemId: Int, quantity: Short, owner: String?, petId: Int, flag: Byte, expiration: Long): Boolean {
            val type = ii.getInventoryType(itemId)
            var quantityInVar = quantity
            if (type != InventoryType.EQUIP) {
                val slotMax = ii.getSlotMax(c, itemId)
                val existing = c.player?.getInventory(type)?.listById(itemId) ?: return false
                if (!ItemConstants.isRechargeable(itemId)) {
                    if (existing.isNotEmpty()) {
                        val i = existing.iterator()
                        while (quantityInVar > 0) {
                            if (i.hasNext()) {
                                val eItem = i.next()
                                val oldQuantity = eItem.quantity
                                if (oldQuantity < slotMax && (eItem.owner == owner) /*|| owner == null*/) {
                                    val newQuantity = (oldQuantity + quantity).coerceAtMost(slotMax.toInt())
                                    quantityInVar = (quantityInVar - (newQuantity - oldQuantity)).toShort()
                                    eItem.quantity = newQuantity.toShort()
                                    eItem.expiration = expiration
                                    c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(1, eItem))))
                                }
                            } else {
                                break
                            }
                        }
                    }
                    while (quantityInVar > 0 || ItemConstants.isRechargeable(itemId)) {
                        val newQuantity = quantity.toInt().coerceAtMost(slotMax.toInt())
                        if (newQuantity != 0) {
                            quantityInVar = quantityInVar.minus(newQuantity).toShort()
                            val newItem = Item(itemId, 0, newQuantity.toShort(), petId)
                            newItem.flag = flag.toInt()
                            newItem.expiration = expiration
                            val newSlot = c.player?.getInventory(type)?.addItem(newItem)
                            if (newSlot?.toInt() == -1) {
                                c.announce(CharacterPacket.getInventoryFull())
                                c.announce(CharacterPacket.getShowInventoryFull())
                                return false
                            }
                            if (owner != null) newItem.owner = owner
                            c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(0, newItem))))
                            if ((ItemConstants.isRechargeable(itemId)) && quantityInVar.toInt() == 0) break
                        } else {
                            c.announce(PacketCreator.enableActions())
                            return false
                        }
                    }
                } else {
                    val newItem = Item(itemId, 0, quantity, petId)
                    newItem.flag = flag.toInt()
                    newItem.expiration = expiration
                    val newSlot = c.player?.getInventory(type)?.addItem(newItem)?.toInt()
                    if (newSlot == -1) {
                        c.announce(CharacterPacket.getInventoryFull())
                        c.announce(CharacterPacket.getShowInventoryFull())
                        return false
                    }
                    c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(0, newItem))))
                }
            } else if (quantity.toInt() == 1) {
                val newEquip = ii.getEquipById(itemId)
                newEquip.flag = flag.toInt()
                newEquip.expiration = expiration
                if (owner != null) newEquip.owner = owner
                val newSlot = c.player?.getInventory(type)?.addItem(newEquip)
                if (newSlot?.toInt() == -1) {
                    c.announce(CharacterPacket.getInventoryFull())
                    c.announce(CharacterPacket.getShowInventoryFull())
                    return false
                }
                c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(0, newEquip))))
            } else {
                return false
            }
            return true
        }

        fun addFromDrop(c: Client, item: Item, show: Boolean): Boolean {
            val type = ii.getInventoryType(item.itemId)
            if (ii.isPickupRestricted(item.itemId) && (c.player?.getItemQuantity(item.itemId, true) ?: 0) > 0) {
                c.announce(CharacterPacket.getInventoryFull())
                c.announce(ItemPacket.showItemUnavailable())
                return false
            }
            var quantity = item.quantity
            if (type != InventoryType.EQUIP) {
                val slotMax = ii.getSlotMax(c, item.itemId)
                val existing = c.player?.getInventory(type)?.listById(item.itemId) ?: listOf()
                if (!ItemConstants.isRechargeable(item.itemId)) {
                    if (existing.isNotEmpty()) {
                        while (quantity > 0) {
                            existing.forEach {
                                val oldQuantity = it.quantity
                                if (oldQuantity < slotMax && item.owner == it.owner) {
                                    val newQuantity = (oldQuantity + quantity).coerceAtMost(slotMax.toInt())
                                    quantity = (quantity - (newQuantity - oldQuantity)).toShort()
                                    it.quantity = newQuantity.toShort()
                                    c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(1, it))))
                                }
                            }
                        }
                    }
                    while (quantity > 0 || ItemConstants.isRechargeable(item.itemId)) {
                        val newQuantity = quantity.toInt().coerceAtMost(slotMax.toInt())
                        quantity = (quantity - newQuantity).toShort()
                        val newItem = Item(item.itemId, 0, newQuantity.toShort())
                        newItem.expiration = item.expiration
                        newItem.owner = item.owner
                        val newSlot = c.player?.getInventory(type)?.addItem(newItem)?.toInt()
                        if (newSlot == -1) {
                            c.announce(CharacterPacket.getInventoryFull())
                            c.announce(CharacterPacket.getShowInventoryFull())
                            return false
                        }
                        c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(0, newItem))))
                        if (ItemConstants.isRechargeable(item.itemId) && quantity.toInt() == 0) break
                    }
                } else {
                    val newItem = Item(item.itemId, 0, quantity)
                    val newSlot = c.player?.getInventory(type)?.addItem(newItem)?.toInt()
                    if (newSlot == -1) {
                        c.announce(CharacterPacket.getInventoryFull())
                        c.announce(CharacterPacket.getShowInventoryFull())
                        return false
                    }
                    c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(0, newItem))))
                    c.announce(PacketCreator.enableActions())
                }
            } else if (quantity.toInt() == 1) {
                val newSlot = c.player?.getInventory(type)?.addItem(item)
                if (newSlot?.toInt() == -1) {
                    c.announce(CharacterPacket.getInventoryFull())
                    c.announce(CharacterPacket.getShowInventoryFull())
                    return false
                }
                c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(0, item))))
            } else return false
            if (show) {
                c.announce(ItemPacket.getShowItemGain(item.itemId, item.quantity))
            }
            return true
        }

        fun checkSpace(c: Client, itemId: Int, quantity: Int, owner: String): Boolean {
            val type = ii.getInventoryType(itemId)
            var quantityInVar = quantity
            if (type == InventoryType.EQUIP) {
                val slotMax = ii.getSlotMax(c, itemId).toInt()
                val existing = c.player?.getInventory(type)?.listById(itemId) ?: listOf()
                if (!ItemConstants.isRechargeable(itemId)) {
                    if (existing.isNotEmpty()) {
                        run loop@{
                            existing.forEach {
                                val oldQuantity = it.quantity.toInt()
                                if (oldQuantity < slotMax && owner == it.owner) {
                                    val newQuantity = (oldQuantity + quantityInVar).coerceAtMost(slotMax)
                                    quantityInVar -= (newQuantity - oldQuantity)
                                }
                                if (quantityInVar <= 0) return@loop
                            }
                        }
                    }
                }
                val numSlotsNeeded = if (slotMax > 0) {
                    ceil(quantityInVar.toDouble() / slotMax).toInt()
                } else 1
                return c.player?.getInventory(type)?.isFull(numSlotsNeeded - 1) == false
            }
            return c.player?.getInventory(type)?.isFull() == false
        }

        fun removeFromSlot(c: Client, type: InventoryType, slot: Byte, quantity: Short, fromDrop: Boolean, consume: Boolean = false) {
            val item = c.player?.getInventory(type)?.getItem(slot) ?: return
            val allowZero = consume && ItemConstants.isRechargeable(item.itemId)
            c.player?.getInventory(type)?.removeItem(slot, quantity, allowZero)
            val mode = if(item.quantity.toInt() == 0 && !allowZero) 3 else 1
            c.announce(CharacterPacket.modifyInventory(fromDrop, listOf(ModifyInventory(mode, item))))
        }

        fun removeById(c: Client, type: InventoryType, itemId: Int, quantity: Int, fromDrop: Boolean, consume: Boolean) {
            val inv = c.player?.getInventory(type) ?: return
            var remRemove = quantity
            for (i in 0..inv.slotLimit) {
                val item = inv.getItem(i.toByte())
                if (item != null && itemId == item.itemId) {
                    if (remRemove <= item.quantity) {
                        removeFromSlot(c, type, item.position, remRemove.toShort(), fromDrop, consume)
                        remRemove = 0
                        break
                    } else {
                        remRemove -= item.quantity
                        removeFromSlot(c, type, item.position, item.quantity, fromDrop, consume)
                    }
                }
            }
            if (remRemove > 0) {
                logger.warn { "Not enough bullet available ($itemId, ${quantity - remRemove}/$quantity)" }
            }
        }

        fun move(c: Client, type: InventoryType, src: Byte, dest: Byte) {
            if (src < 0 || dest < 0) return
            val source = c.player?.getInventory(type)?.getItem(src)
            val initialTarget = c.player?.getInventory(type)?.getItem(dest)
            if (source == null) return
            var oldDestQuantity = -1
            if (initialTarget != null) {
                oldDestQuantity = initialTarget.quantity.toInt()
            }
            val oldSrcQuantity = source.quantity
            val slotMax = ii.getSlotMax(c, source.itemId)
            c.player?.getInventory(type)?.move(src, dest, slotMax)
            val mods = mutableListOf<ModifyInventory>()
            if (type != InventoryType.EQUIP && initialTarget != null && initialTarget.itemId == source.itemId && !ItemConstants.isRechargeable(source.itemId)) {
                val mod = if ((oldDestQuantity + oldSrcQuantity) > slotMax) 1 else 3
                mods.add(ModifyInventory(mod, source))
                mods.add(ModifyInventory(1, initialTarget))
            } else {
                mods.add(ModifyInventory(2, source, src.toShort()))
            }
            c.announce(CharacterPacket.modifyInventory(true, mods))
        }

        fun equip(c: Client, src: Byte, dest: Byte) {
            c.player?.let { player ->
                var source = player.getInventory(InventoryType.EQUIP)?.getItem(src) as Equip?
                if (source == null || !ii.canWearEquipment(player, source)) {
                    c.announce(PacketCreator.enableActions())
                    return
                }
                if ((source.itemId in 1902000..1902002 || source.itemId == 1912000) || (source.itemId in 1902005..1902007 || source.itemId == 1912005)) { // Adventurer taming equipment
                    return
                }
                var itemChanged = false
                if (ii.isUnTradeableOnEquip(source.itemId)) {
                    source.flag = ItemConstants.UNTRADEABLE
                    itemChanged = true
                }
                //if (source.ringId > -1) player.getRingById(source.ringId).equipped = true
                fun isFullAndSend(): Boolean {
                    return if (player.getInventory(InventoryType.EQUIP)?.isFull() == true) {
                        c.announce(CharacterPacket.getInventoryFull())
                        c.announce(CharacterPacket.getShowInventoryFull())
                        true
                    } else false
                }

                val inv = player.getInventory(InventoryType.EQUIPPED)
                when (dest.toInt()) {
                    -6 -> {
                        val top = inv?.getItem(-5)
                        if (top != null && isOverall(top.itemId)) {
                            val isFull = isFullAndSend()
                            if (isFull) return
                            player.getInventory(InventoryType.EQUIP)?.getNextFreeSlot()?.let { unEquip(c, -5, it) }
                        }
                    }
                    -5 -> {
                        val bottom = inv?.getItem(-6)
                        if (bottom != null && isOverall(source.itemId)) {
                            if (isFullAndSend()) return
                            player.getInventory(InventoryType.EQUIP)?.getNextFreeSlot()?.let { unEquip(c, -6, it) }
                        }
                    }
                    -10 -> {
                        val weapon = inv?.getItem(-11)
                        if (weapon != null && ii.isTwoHanded(weapon.itemId)) {
                            if (isFullAndSend()) return
                            player.getInventory(InventoryType.EQUIP)?.getNextFreeSlot()?.let { unEquip(c, -11, it) }
                        }
                    }
                    -11 -> {
                        val shield = inv?.getItem(-10)
                        if (shield != null && ii.isTwoHanded(source.itemId)) {
                            if (isFullAndSend()) return
                            player.getInventory(InventoryType.EQUIP)?.getNextFreeSlot()?.let { unEquip(c, -10, it) }
                        }
                    }
                }
                if (dest.toInt() == -18) {
                    if (player.mount != null) {
                        player.mount?.itemId = source.itemId
                    }
                }
                if (source.itemId == 1122017) player.equipPendantOfSpirit()
                source = player.getInventory(InventoryType.EQUIP)?.getItem(src) as Equip?
                val target = player.getInventory(InventoryType.EQUIPPED)?.getItem(dest) as Equip?
                player.getInventory(InventoryType.EQUIP)?.removeSlot(src)
                if (target != null) player.getInventory(InventoryType.EQUIPPED)?.removeSlot(dest)
                val mods = mutableListOf<ModifyInventory>()
                if (itemChanged && source != null) {
                    mods.add(ModifyInventory(3, source))
                    mods.add(ModifyInventory(0, source.copy())) // To prevent crashes.
                }
                source?.position = dest
                if (source != null) {
                    player.getInventory(InventoryType.EQUIPPED)?.addFromDatabase(source)
                }
                if (target != null) {
                    target.position = src
                    player.getInventory(InventoryType.EQUIP)?.addFromDatabase(target)
                }
                if (player.getBuffedValue(BuffStat.BOOSTER) != null && isWeapon(source?.itemId ?: 0)) {
                    player.cancelBuffStats(BuffStat.BOOSTER)
                }
                source?.let { mods.add(ModifyInventory(2, source, src.toShort())) }
                c.announce(CharacterPacket.modifyInventory(true, mods))
                player.equipChanged()
            }
        }

        fun unEquip(c: Client, src: Byte, dest: Byte) {
            val source = c.player?.getInventory(InventoryType.EQUIPPED)?.getItem(src)
            val target = c.player?.getInventory(InventoryType.EQUIP)?.getItem(dest)
            source as? Equip
            target as? Equip
            if (dest < 0) return
            if (source == null) return
            if (target != null && src <= 0) {
                c.announce(CharacterPacket.getInventoryFull())
            }
            if (source.itemId == 1122017) c.player?.unEquipPendantOfSpirit()
            //if (source.ringId > -1) c.player?.getRingById(source.ringId).equipped = false
            c.player?.getInventory(InventoryType.EQUIPPED)?.removeSlot(src)
            if (target != null) {
                c.player?.getInventory(InventoryType.EQUIP)?.removeSlot(dest)
            }
            source.position = dest
            c.player?.getInventory(InventoryType.EQUIP)?.addFromDatabase(source)
            if (target != null) {
                target.position = src
                c.player?.getInventory(InventoryType.EQUIPPED)?.addFromDatabase(target)
            }
            c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(2, source, src.toShort()))))
            c.player?.equipChanged()
        }

        fun drop(c: Client, _type: InventoryType, src: Byte, quantity: Short) {
            c.player?.let { player ->
                val type = if (src < 0) InventoryType.EQUIPPED else _type
                val source = player.getInventory(type)?.getItem(src)
                val itemId = source?.itemId ?: return
                if (itemId in 5000000..5000100) return
                val flag = source.flag
                if (flag != ItemConstants.getFlagByInt(type.type.toInt())) return
                if (type == InventoryType.EQUIPPED && itemId == 1122017) player.unEquipPendantOfSpirit()
                if (player.itemEffect == itemId && source.quantity.toInt() == 1) {
                    player.itemEffect = 0
                    player.let { it.map.broadcastMessage(ItemPacket.itemEffect(it.id, 0)) }
                } else if (itemId == 5370000 || itemId == 5370001) {
                    if (player.getItemQuantity(itemId, false) == 1) player.chalkBoard = ""
                }
                if (player.getItemQuantity(itemId, true) < quantity || quantity < 0 || quantity.toInt() == 0 && !ItemConstants.isRechargeable(itemId)) return
                val dropPos = Point(player.position)
                if (quantity < source.quantity && !ItemConstants.isRechargeable(itemId)) {
                    val target = source.copy()
                    target.quantity = quantity
                    source.quantity = (source.quantity - quantity).toShort()
                    c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(1, source))))
                    val weddingRing = source.itemId == 1112803 || source.itemId == 1112806 || source.itemId == 1112807 || source.itemId == 1112809
                    if (ii.isDropRestricted(target.itemId) || ii.isCash(target.itemId) || weddingRing) {
                        player.map.disappearingItemDrop(player, player, target, dropPos)
                    } else {
                        val playerDrop = !player.map.everLast
                        player.map.spawnItemDrop(player, player, source, dropPos, true, playerDrop)
                    }
                } else {
                    player.getInventory(type)?.removeSlot(src)
                    c.announce(CharacterPacket.modifyInventory(true, listOf(ModifyInventory(3, source))))
                    if (src < 0) player.equipChanged()
                    if (ii.isDropRestricted(itemId)) {
                        player.map.disappearingItemDrop(player, player, source, dropPos)
                    } else {
                        val playerDrop = !player.map.everLast
                        player.map.spawnItemDrop(player, player, source, dropPos, true, playerDrop)
                    }
                }
            }
        }

        private fun isOverall(itemId: Int) = itemId / 10000 == 105

        private fun isWeapon(itemId: Int) = itemId in 1302000..1492023
    }
}