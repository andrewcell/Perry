package tools.settings

data class ShopItemDatabase(
    //  {"shopItemId":3355,"shopId":9999999,"itemId":1902001,"price":1,"pitch":0,"position":328},
    val shopItemId: Int,
    val shopId: Int,
    val itemId: Int,
    val price: Int,
    val pitch: Int,
    val position: Int
)
