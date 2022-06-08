package server

object ShopFactory {
     private val shops = mutableMapOf<Int, Shop?>()
     private val npcShops = mutableMapOf<Int, Shop?>()

    fun getShop(shopId: Int): Shop? {
        val shop = shops[shopId]
        if (shop != null) {
            return shop
        }
        return loadShop(shopId, true)
    }

    fun getShopForNpc(npcId: Int): Shop? {
        val npcShop = npcShops[npcId]
        if (npcShop != null) {
            return npcShop
        }
        return loadShop(npcId, false)
    }

    private fun loadShop(id: Int, isShopId: Boolean): Shop? {
        val ret = Shop.createFromDatabase(id, isShopId)
        if (ret != null) {
            shops[ret.id] = ret
            npcShops[ret.npcId] = ret
        } else if (isShopId) {
            shops[id] = null
        } else {
            npcShops[id] = null
        }
        return ret
    }

    //fun reloadShops() = shops.clear()
}