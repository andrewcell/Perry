package server

data class ShopItem(
    val buyable: Short,
    val itemId: Int,
    val price: Int,
    val pitch: Int
)