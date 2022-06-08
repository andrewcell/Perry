package server

import client.inventory.Item

data class PlayerShopItem(
    val item: Item,
    var bundles: Short,
    val price: Int
) {
    var doesExist: Boolean = true
}
