package server

import client.Client
import client.inventory.InventoryType
import client.inventory.Pet
import com.beust.klaxon.Klaxon
import constants.ItemConstants
import mu.KLogging
import tools.PacketCreator
import tools.ResourceFile
import tools.packet.InteractPacket
import tools.packet.NpcPacket
import tools.settings.ShopDatabase
import tools.settings.ShopItemDatabase
import kotlin.math.ceil
import kotlin.math.roundToInt

class Shop(val id: Int, val npcId: Int) {
    private val items = mutableListOf<ShopItem>()
    private val token = 4000313
    private val tokenValue = 1000000000

    fun addItem(item: ShopItem) = items.add(item)

    fun sendShop(c: Client) {
        c.player?.shop = this
        c.announce(NpcPacket.getNpcShop(c, npcId, items))

    }

    fun buy(c: Client, slot: Short, itemId: Int, quantity: Short) {
        val item = items.getOrNull(slot.toInt())
        if (item != null) {
            if (item.itemId != itemId) {
                logger.warn { "Wrong slot number in shop $id" }
                return
            }
        } else return
        val ii = ItemInformationProvider
        c.player?.let { player ->
            if (item.price > 0) {
                if (player.meso.get() >= item.price * quantity) {
                    if (InventoryManipulator.checkSpace(c, itemId, quantity.toInt(), "")) {
                        if (!ItemConstants.isRechargeable(itemId)) {
                            InventoryManipulator.addById(c, itemId, quantity)
                            player.gainMeso(-(item.price * quantity), false)
                        } else {
                            val slotMax = ii.getSlotMax(c, item.itemId)
                            InventoryManipulator.addById(c, itemId, slotMax)
                            player.gainMeso(-item.price, false)
                        }
                        c.announce(NpcPacket.shopTransaction(0))
                    } else {
                        c.announce(NpcPacket.shopTransaction(3))
                    }
                } else {
                    c.announce(NpcPacket.shopTransaction(2))
                }
            } else if (item.pitch > 0) {
                if ((player.getInventory(InventoryType.ETC)?.countById(4310000) ?: 0) >= (item.pitch * quantity)) {
                    if (InventoryManipulator.checkSpace(c, itemId, quantity.toInt(), "")) {
                        var quantityToAdd = quantity
                        if (ItemConstants.isRechargeable(itemId)) {
                            quantityToAdd = ii.getSlotMax(c, item.itemId)
                        }
                        InventoryManipulator.addById(c, itemId, quantity)
                        InventoryManipulator.removeById(c, InventoryType.ETC, 4310000, item.pitch * quantityToAdd,
                            fromDrop = false,
                            consume = false
                        )
                        c.announce(NpcPacket.shopTransaction(0))
                    } else {
                        c.announce(NpcPacket.shopTransaction(3))
                    }
                }
            } else if (player.getInventory(InventoryType.CASH)?.countById(token) != 0) {
                val amount = player.getInventory(InventoryType.CASH)?.countById(token) ?: 0
                val value = amount + tokenValue
                val cost = item.price * quantity
                if (player.meso.get() + value >= cost) {
                    val cardReduce = value - cost
                    val diff = cardReduce + player.meso.get()
                    if (InventoryManipulator.checkSpace(c, itemId, quantity.toInt(), "")) {
                        if (itemId in 5000000..5000100) {
                            val petId = Pet.createPet(itemId)
                            InventoryManipulator.addById(c, itemId, quantity, null, petId, -1)
                        } else {
                            InventoryManipulator.addById(c, itemId, quantity)
                        }
                        player.gainMeso(diff, false)
                    } else {
                        c.announce(NpcPacket.shopTransaction(3))
                    }
                    c.announce(NpcPacket.shopTransaction(0))
                } else {
                    c.announce(NpcPacket.shopTransaction(2))
                }
            }
        }
    }

    fun sell(c: Client, type: InventoryType, slot: Short, quantity: Short) {
        var quantityToSell = quantity.toInt()
        if (quantityToSell == 0) {
            quantityToSell = 1
        }
        val ii = ItemInformationProvider
        val item = c.player?.getInventory(type)?.getItem(slot.toByte()) ?: return
        if (ItemConstants.isRechargeable(item.itemId)) {
            quantityToSell = item.quantity.toInt()
        }
        if (quantityToSell < 0) return
        val itemQuantity = item.quantity
        if (quantityToSell <= itemQuantity && itemQuantity > 0) {
            InventoryManipulator.removeFromSlot(c, type, slot.toByte(), quantityToSell.toShort(), false)
            val price: Double = if (ItemConstants.isRechargeable(item.itemId)) {
                ii.getWholePrice(item.itemId) / ii.getSlotMax(c, item.itemId).toDouble()
            } else {
                ii.getPrice(item.itemId)
            }
            val receiveMesos = ceil(price * quantity).coerceAtLeast(0.0)
            if (price != 1.toDouble() && receiveMesos > 0) {
                c.player?.gainMeso(receiveMesos.toInt(), false)
            }
            c.announce(NpcPacket.shopTransaction(0x8))
        }
    }

    fun recharge(c: Client, slot: Byte) {
        val ii = ItemInformationProvider
        val item = c.player?.getInventory(InventoryType.USE)?.getItem(slot)
        if (item == null || !ItemConstants.isRechargeable(item.itemId)) return
        val slotMax = ii.getSlotMax(c, item.itemId)
        if (item.quantity < 0) return
        if (item.quantity < slotMax) {
            val price = (ii.getPrice(item.itemId) * (slotMax - item.quantity)).roundToInt()
            c.player?.let { player ->
                if (player.meso.get() >= price) {
                    item.quantity = slotMax
                    player.forceUpdateItem(item)
                    player.gainMeso(-price, show = false, enableActions = true)
                    c.announce(NpcPacket.shopTransaction(0x8))
                } else {
                    c.announce(InteractPacket.serverNotice(1, "메소가 부족합니다."))
                    c.announce(PacketCreator.enableActions())
                }
            }
        }
    }

    companion object : KLogging() {
        private val rechargeableItems = mutableSetOf<Int>()

        init {
            for (i in 2070000..2070013) { //~13
                rechargeableItems.add(i)
            }
            rechargeableItems.remove(2070007) // 화비
        }

        fun createFromDatabase(id: Int, isShopId: Boolean): Shop? {
            var ret: Shop? = null
            val shopId: Int
            val shopData = ResourceFile.load("shops.json")?.let { Klaxon().parseArray<ShopDatabase>(it) } ?: return null
            val shopItemData = ResourceFile.load("shopItem.json")?.let { Klaxon().parseArray<ShopItemDatabase>(it) } ?: return null
            try {
                val filtered = shopData.filter { if (isShopId) it.shopId == id else it.npcId == id }
                if (filtered.isEmpty()) return null
                shopId = filtered.first().shopId
                ret = Shop(shopId, filtered.first().npcId)
                val filteredItemData = shopItemData.filter { it.shopId == shopId }.sortedBy { it.position } //desc
                val recharges = rechargeableItems.toMutableList()
                filteredItemData.forEach {
                    if (ItemConstants.isRechargeable(it.itemId)) {
                        val starItem = ShopItem(1, it.itemId, it.price, it.pitch)
                        ret.addItem(starItem)
                        recharges.remove(starItem.itemId)
                    } else {
                        ret.addItem(ShopItem(1000, it.itemId, it.price, it.pitch))
                    }
                }
                recharges.forEach { ret.addItem(ShopItem(100, it, 0, 0)) }
            } catch (e: Exception) {
                logger.error(e) { "Failed to load shop data from resource files. ShopId: $id" }
            }
            return ret
        }
    }
}