package client.inventory

import constants.ItemConstants

class ModifyInventory(val mode: Int, var item: Item, val oldPosition: Short = -1) {
    fun getInventoryType() = item.let { ItemConstants.getInventoryType(it.itemId).type }

    fun getPosition() = item.position

    fun getQuantity() = item.quantity
}