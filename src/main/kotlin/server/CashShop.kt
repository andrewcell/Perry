package server

import client.inventory.*
import constants.ItemConstants
import database.Accounts
import database.Gifts
import database.SpecialCashItems
import database.Wishlists
import mu.KLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import provider.DataProviderFactory
import provider.DataTool
import tools.ServerJSON.settings
import java.io.File
import java.sql.SQLException
import kotlin.random.Random
import server.ItemInformationProvider as ii

class CashShop(val accountId: Int, val characterId: Int) {
    data class CashItem(val sn: Int, val itemId: Int, val price: Int, val period: Long, val count: Short, val onSale: Boolean) {
        fun toItem(): Item {
            val petId = if (ItemConstants.isPet(itemId)) Pet.createPet(itemId) else -1
            val item = if (ii.getInventoryType(itemId) == InventoryType.EQUIP) {
                ii.getEquipById(itemId)
            } else {
                Item(itemId, 0, count, petId)
            }
            if (ItemConstants.EXPIRING_ITEMS && period > 0) {
                item.expiration = System.currentTimeMillis() + 1000 * 60 * 60 * if (period == 1L) 4 else 24 * period
            }
            item.sn = sn
            item.cashId = Random.nextInt(0, Int.MAX_VALUE)
            return item
        }
    }
    data class SpecialCashItem(val sn: Int, val modifier: Int, val info: Byte)

    class CashItemFactory {
        companion object : KLogging() {
            val items = mutableMapOf<Int, CashItem>()
            private val packages = mutableMapOf<Int, List<Int>>()
            private val specialCashItems = mutableListOf<SpecialCashItem>()

            init {
                logger.debug { "Loading cash items" }
                val etc = DataProviderFactory.getDataProvider(File("${settings.wzPath}/Etc.wz"))
                etc.getData("Commodity.img")?.children?.forEach { item ->
                    val sn = DataTool.getIntConvert("SN", item)
                    val cashItem =  CashItem(
                        sn,
                        DataTool.getIntConvert("ItemId", item, 4000000),
                        DataTool.getIntConvert("Price", item, 0),
                        DataTool.getIntConvert("Period", item, 1).toLong(),
                        DataTool.getIntConvert("Count", item, 1).toShort(),
                        DataTool.getIntConvert("OnSale", item, 0) == 1
                    )
                    items[sn] = cashItem
                    with (cashItem) {
                        logger.trace { "Commodity.img - SN:$sn - $itemId - ${ii.getName(itemId)}" }
                    }
                }
                etc.getData("CashPackage.img")?.children?.forEach {
                    logger.trace { "CashPackage.img - ${it.name}"}
                    val cPackage = mutableListOf<Int>()
                    it.getChildByPath("SN")?.children?.forEach { item ->
                        cPackage.add(item.data.toString().toInt())
                    }
                    packages[it.name.toInt()] = cPackage
                }
                try {
                    transaction {
                        SpecialCashItems.selectAll().forEach {
                            specialCashItems.add(SpecialCashItem(it[SpecialCashItems.id], it[SpecialCashItems.modifier], it[SpecialCashItems.info].toByte()))
                        }
                    }
                } catch (e: SQLException) {
                    logger.error(e) { "Failed to save special cash items to database." }
                }
            }

            fun getItem(sn: Int) = items[sn]

            fun getPackage(itemId: Int): List<Item> {
                val cashPackage = mutableListOf<Item>()
                packages[itemId]?.forEach { sn ->
                    getItem(sn)?.toItem()?.let { it -> cashPackage.add(it) }
                }
                return cashPackage
            }

            fun isPackage(itemId: Int) = packages.containsKey(itemId)

        }
    }

    private var nxCredit = 0
    private var nxPrepaid = 0
    private var mPoint = 0
    var notes = 0
    val inventory = mutableListOf<Item>()
    val wishList = mutableListOf<Int>()
    private val factory = ItemFactory.CASH_EXPLORER
    var opened = false

    init {
        try {
            logger.debug { "Cash shop opened. Account Id: $accountId, Character Id: $characterId" }
            transaction {
                val row = Accounts.slice(Accounts.nxCredit, Accounts.mPoint, Accounts.nxPrepaid).select {
                    Accounts.id eq accountId
                }
                if (!row.empty()) {
                    row.first().let {
                        nxCredit = it[Accounts.nxCredit] ?: 0
                        nxPrepaid = it[Accounts.nxPrepaid] ?: 0
                        mPoint = it[Accounts.mPoint] ?: 0
                    }
                }
                factory.loadItems(accountId, false).forEach {
                    inventory.add(it.first)
                }
                Wishlists.slice(Wishlists.sn).select {
                    Wishlists.charId eq characterId
                }.forEach {
                    wishList.add(it[Wishlists.sn])
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to init cash shop. AccountId: $accountId, CharacterId: $characterId" }
        }
    }

    fun getCash(type: Int) = when (type) {
        1 -> nxCredit
        2 -> mPoint
        4 -> nxPrepaid
        else -> 0
    }

    fun gainCash(type: Int, cash: Int) = when (type) {
        1 -> nxCredit += cash
        2 -> mPoint += cash
        4 -> nxPrepaid += cash
        else -> nxPrepaid += 0
    }

    fun findByCashId(cashId: Int): Item? {
        var isRing = false
        var equip: Equip? = null
        inventory.forEach { item ->
            if (item.getType() == 1) {
                equip = item as Equip
                isRing = equip?.let { it.ringId > -1 } ?: false
            }
            if ((if (item.petId > -1) item.petId else if (isRing) equip?.ringId else item.cashId) == cashId) {
                logger.debug { "Character id $characterId found item ${item.itemId} by cash Id $cashId" }
                return item
            }
        }
        return null
    }

    fun addToInventory(item: Item) = inventory.add(item)

    fun removeFromInventory(item: Item) = inventory.remove(item)

    fun addToWishList(sn: Int) = wishList.add(sn)

    fun gift(recipient: Int, from: String, message: String, sn: Int, ringId: Int = -1) {
        try {
            transaction {
                Gifts.insert {
                    it[to] = recipient
                    it[Gifts.from] = from
                    it[Gifts.message] = message
                    it[Gifts.sn] = sn
                    it[Gifts.ringId] = ringId
                }
            }
            logger.debug { "Cash shop gift added. Recipient: $recipient, From: $from, SN: $sn" }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to insert gift." }
        }
    }

    fun loadGifts(): List<Pair<Item, String>> {
        val gifts = mutableListOf<Pair<Item, String>>()
        logger.debug { "Loading cash shop gifts. character id: $characterId" }
        try {
            transaction {
                Gifts.select { Gifts.to eq characterId }.forEach {
                    notes++
                    val cashItem = CashItemFactory.getItem(it[Gifts.sn]) ?: return@transaction
                    val item = cashItem.toItem()
                    item.giftFrom = it[Gifts.from]
                    if (item.getType() == InventoryType.EQUIP.type.toInt()) {
                        item as Equip
                        item.ringId = it[Gifts.ringId]
                    }
                    logger.trace { "Find cash shop gift item ${item.itemId}, From: ${item.giftFrom}, character id: $characterId" }
                    gifts.add(Pair(item, it[Gifts.message]))
                    if (CashItemFactory.isPackage(cashItem.itemId)) {
                        CashItemFactory.getPackage(cashItem.itemId).forEach { pItem ->
                            pItem.giftFrom = it[Gifts.from]
                            addToInventory(pItem)
                        }
                    } else {
                        addToInventory(item)
                    }
                }
                Gifts.deleteWhere { Gifts.to eq characterId }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to load cash shop gifts." }
        }
        return gifts
    }

    fun decreaseNotes() = notes--

    fun save() {
        logger.debug { "Saving cash shop character id $characterId, account id $accountId" }
        transaction {
            Accounts.update({ Accounts.id eq accountId }) {
                it[nxCredit] = this@CashShop.nxCredit
                it[nxPrepaid] = this@CashShop.nxPrepaid
                it[mPoint] = this@CashShop.mPoint
            }
            val itemsWithType = mutableListOf<Pair<Item, InventoryType>>()
            inventory.forEach { item ->
                itemsWithType.add(Pair(item, ii.getInventoryType(item.itemId)))
            }
            factory.saveItems(itemsWithType, accountId)
            Wishlists.deleteWhere { Wishlists.charId eq characterId }
            wishList.forEach { sn ->
                Wishlists.insert {
                    it[charId] = characterId
                    it[Wishlists.sn] = sn
                }
            }
        }
    }

    companion object : KLogging()
}