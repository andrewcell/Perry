package client.inventory

import constants.ItemConstants

/**
 * Represents an inventory in the game.
 *
 * @property type The type of the inventory. This can be any value from the `InventoryType` enum.
 * @property slotLimit The maximum number of slots in the inventory. Default value is 96.
 * @constructor Creates an instance of Inventory which is also an Iterable of `Item`.
 */
class Inventory(val type: InventoryType, var slotLimit: Byte = 96) : Iterable<Item> {
    // Represents the items in the inventory. The key is the slot number and the value is the item in that slot.
    val inventory = mutableMapOf<Byte, Item>()

    // A flag to indicate whether the inventory has been checked or not.
    var checked = false

    /**
     * Checks if the inventory is extendable.
     *
     * An inventory is considered extendable if its type is not UNDEFINED, EQUIPPED, or CASH.
     *
     * @return true if the inventory is extendable, false otherwise.
     */
    fun isExtendableInventory() = when (type) {
        InventoryType.UNDEFINED, InventoryType.EQUIPPED, InventoryType.CASH -> false
        else -> true
    }

    /**
     * Checks if the inventory is of type EQUIP or EQUIPPED.
     *
     * @return true if the inventory is of type EQUIP or EQUIPPED, false otherwise.
     */
    fun isEquipInventory() = type == InventoryType.EQUIP || type == InventoryType.EQUIPPED

    /**
     * Finds an item in the inventory by its ID.
     *
     * @param itemId The ID of the item to find.
     * @return The item if found, null otherwise.
     */
    fun findById(itemId: Int) = inventory.values.find { it.itemId == itemId }

    /**
     * Counts the number of items in the inventory with a specific ID.
     *
     * @param itemId The ID of the item to count.
     * @return The count of items with the specified ID.
     */
    fun countById(itemId: Int) = inventory.values.count { it.itemId == itemId }

    /**
     * Lists all items in the inventory with a specific ID.
     *
     * @param itemId The ID of the item to list.
     * @return A sorted list of items with the specified ID.
     */
    fun listById(itemId: Int) = inventory.values.filter { it.itemId == itemId }.toList().sorted()

    /**
     * Lists all items in the inventory.
     *
     * @return A list of all items in the inventory.
     */
    fun list() = inventory.values.toList()
    fun addItem(item: Item?): Byte {
        val slotId = getNextFreeSlot()
        if (slotId < 0 || item == null) return -1
        inventory[slotId] = item
        item.position = slotId
        return slotId
    }

    /**
     * Adds an item to the inventory from the database.
     *
     * This function is typically used when loading the inventory from the database. It checks if the item's position is not negative and the inventory type is not EQUIPPED. If these conditions are met, the item is added to the inventory at its position.
     *
     * @param item The item to be added to the inventory.
     */
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

    /**
     * Finds an item in the inventory by its cash ID.
     *
     * This function is used to find an item in the inventory by its cash ID.
     * It first checks if the item is of type `Equip`. If it is, it checks if the item is a ring.
     * If the item is a ring, it uses the ring ID. If the item is not a ring, it uses the cash ID.
     * If the item is not of type `Equip`, it checks if the item is a pet. If it is, it uses the pet ID.
     * If it is not, it uses the cash ID.
     *
     * @param cashId The cash ID of the item to find.
     * @return The item if found, null otherwise.
     */
    fun findByCashId(cashId: Int) = inventory.values.find {
        var isRing = false
        var equip: Equip? = null
        if (it.getType() == 1) {
            equip = it as Equip
            isRing = equip.ringId > -1
        }
        (if (it.petId > -1) it.petId else if (isRing) equip?.ringId else it.cashId) == cashId
    }

    /**
     * Provides an iterator over the items in the inventory.
     *
     * This function is used to iterate over the items in the inventory. It returns an iterator over the values in the inventory map, which are the items in the inventory.
     *
     * @return An iterator over the items in the inventory.
     */
    override fun iterator() = inventory.values.iterator()
}