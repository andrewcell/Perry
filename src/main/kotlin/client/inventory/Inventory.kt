package client.inventory

import constants.ItemConstants

class Inventory(val type: InventoryType, var slotLimit: Byte = 96) : Iterable<Item> {
    val inventory = mutableMapOf<Byte, Item>()
    var checked = false

    fun isExtendableInventory() = when (type) {
        InventoryType.UNDEFINED, InventoryType.EQUIPPED, InventoryType.CASH -> false
        else -> true
    }

    fun isEquipInventory() = type == InventoryType.EQUIP || type == InventoryType.EQUIPPED

    fun findById(itemId: Int) = inventory.values.find { it.itemId == itemId }

    fun countById(itemId: Int) = inventory.values.count { it.itemId == itemId }

    fun listById(itemId: Int) = inventory.values.filter { it.itemId == itemId }.toList().sorted()

    fun list() = inventory.values.toList()

    fun addItem(item: Item?): Byte {
        val slotId = getNextFreeSlot()
        if (slotId < 0 || item == null) return -1
        inventory[slotId] = item
        item.position = slotId
        return slotId
    }

    fun addFromDatabase(item: Item) {
        if (item.position < 0 && type != InventoryType.EQUIPPED) return
        inventory[item.position] = item
    }

    fun move(sSlot: Byte, dSlot: Byte, slotMax: Short) {
        val source = inventory[sSlot]
        val target = inventory[dSlot]
        if (source == null) return
        if (target == null) {
            source.position = dSlot
            inventory[dSlot] = source
            inventory.remove(sSlot)
        } else if (target.itemId == source.itemId && !ItemConstants.isRechargeable(source.itemId)) {
            if (type.type == InventoryType.EQUIP.type) swap(target, source)
            if (source.quantity + target.quantity > slotMax) {
                val rest = (source.quantity + target.quantity) - slotMax
                source.quantity = rest.toShort()
                target.quantity = slotMax
            } else {
                target.quantity = (source.quantity + target.quantity).toShort()
                inventory.remove(sSlot)
            }
        } else {
            swap(target, source)
        }
    }

    private fun swap(source: Item, target: Item) {
        inventory.remove(source.position)
        inventory.remove(target.position)
        val swapPos = source.position
        source.position = target.position
        target.position = swapPos
        inventory[source.position] = source
        inventory[target.position] = target
    }

    fun getItem(slot: Byte) = inventory[slot]

    fun removeItem(slot: Byte, quantity: Short = 1, allowZero: Boolean = false) {
        val item = inventory[slot] ?: return
        item.quantity = (item.quantity - quantity).toShort()
        if (item.quantity < 0) item.quantity = 0
        if (item.quantity.toInt() == 0 && !allowZero) removeSlot(slot)
    }

    fun removeSlot(slot: Byte) = inventory.remove(slot)

    fun isFull(margin: Int = 0) = inventory.size + margin >= slotLimit

    fun getNextFreeSlot(): Byte {
        if (isFull()) {
            return -1
        }
        for (i in 1..slotLimit) {
            if (inventory[i.toByte()] == null) {
                return i.toByte()
            }
        }
        return -1
    }

    fun getNumFreeSlot(): Byte {
        if (isFull()) return 0
        var free: Byte = 0
        for (i in 1..slotLimit) {
            if (!inventory.keys.contains(i.toByte())) {
                free++
            }
        }
        return free
    }

    fun allInventories() = listOf(this)

    fun findByCashId(cashId: Int) = inventory.values.find {
            var isRing = false
            var equip: Equip? = null
            if (it.getType() == 1) {
                equip = it as Equip
                isRing = equip.ringId > -1
            }
            (if (it.petId > -1) it.petId else if (isRing) equip?.ringId else it.cashId) == cashId
        }

    override fun iterator() = inventory.values.iterator()
}