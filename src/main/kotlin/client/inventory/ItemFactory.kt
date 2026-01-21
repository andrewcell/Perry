package client.inventory

import client.inventory.ItemFactory.*
import database.InventoryEquipment
import database.InventoryItems
import mu.KLogging
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.SQLException

/**
 * Enum class representing the different types of item factories in the game.
 *
 * Each enum constant represents a different type of item factory and is associated with an integer value and a boolean value.
 * The integer value is used to identify the type of item factory.
 * The boolean value indicates whether the item factory is associated with an account.
 *
 * @property value The integer value associated with the item factory.
 * @property account The boolean value indicating whether the item factory is associated with an account.
 * @property INVENTORY Represents an inventory item factory. It is not associated with an account.
 * @property STORAGE Represents a storage item factory. It is associated with an account.
 * @property CASH_EXPLORER Represents a cash explorer item factory. It is associated with an account.
 * @property MERCHANT Represents a merchant item factory. It is not associated with an account.
 * @see ItemFactory.loadItems for the method that loads items from the item factory.
 * @see ItemFactory.saveItems for the method that saves items to the item factory.
 */
enum class ItemFactory(val value: Int, val account: Boolean) {
    INVENTORY(1, false),
    STORAGE(2, true),
    CASH_EXPLORER(3, true),
    MERCHANT(6, false);

    /**
     * Function to load items from the database.
     *
     * This function takes an ID and a boolean value as parameters. It then loads items from the database associated with the given ID and item factory type.
     * It first selects items from the InventoryItems table joined with the InventoryEquipment table.
     * If the item factory is associated with an account, it selects items where the account ID is equal to the given ID.
     * Otherwise, it selects items where the character ID is equal to the given ID.
     * If the login parameter is true, it further filters the items to only include those of type EQUIPPED.
     * It then iterates over the selected items and creates an Item or Equip object for each item, depending on the item type.
     * It adds each item to a list along with its InventoryType, and returns this list.
     *
     * @param id The ID associated with the items. This can be either an account ID or a character ID, depending on the type of item factory.
     * @param login A boolean value indicating whether to only load items of type EQUIPPED.
     * @return A list of pairs, where each pair consists of an Item or Equip object and an InventoryType object.
     * @throws SQLException If an SQL error occurs while loading the items from the database.
     * @see Item for the class that represents an item.
     * @see Equip for the class that represents an equip item.
     * @see InventoryType for the enum class that represents the type of item.
     * @see ItemFactory for the enum class that represents the type of item factory.
     * @see ItemFactory.saveItems for the method that saves items to the item factory.
     */
    fun loadItems(id: Int, login: Boolean): List<Pair<Item, InventoryType>> {
        val items = mutableListOf<Pair<Item, InventoryType>>()
        try {
            transaction {
                var rs = (InventoryItems leftJoin InventoryEquipment).selectAll().where {
                    if (account) {
                        InventoryItems.accountId eq id
                    } else {
                        InventoryItems.characterId eq id
                    }
                }
                if (login) {
                    rs = rs.andWhere { InventoryItems.inventoryType eq InventoryType.EQUIPPED.type.toInt() }
                }
                rs.forEach {
                    val mit = InventoryType.getByType(it[InventoryItems.inventoryType].toByte())
                    if (mit == InventoryType.EQUIP || mit == InventoryType.EQUIPPED) {
                        val equip = Equip(it[InventoryItems.itemId], it[InventoryItems.position].toByte())
                        equip.owner = it[InventoryItems.owner]
                        equip.quantity = it[InventoryItems.quantity].toShort()
                        equip.acc = it[InventoryEquipment.acc].toShort()
                        equip.avoid = it[InventoryEquipment.avoid].toShort()
                        equip.dex = it[InventoryEquipment.dex].toShort()
                        equip.hands = it[InventoryEquipment.hands].toShort()
                        equip.hp = it[InventoryEquipment.hp].toShort()
                        equip.int = it[InventoryEquipment.int].toShort()
                        equip.jump = it[InventoryEquipment.jump].toShort()
                        equip.vicious = it[InventoryEquipment.vicious].toShort()
                        equip.flag = it[InventoryItems.flag]
                        equip.luk = it[InventoryEquipment.luk].toShort()
                        equip.matk = it[InventoryEquipment.matk].toShort()
                        equip.mdef = it[InventoryEquipment.mdef].toShort()
                        equip.mp = it[InventoryEquipment.mp].toShort()
                        equip.speed = it[InventoryEquipment.speed].toShort()
                        equip.str = it[InventoryEquipment.str].toShort()
                        equip.watk = it[InventoryEquipment.watk].toShort()
                        equip.wdef = it[InventoryEquipment.wdef].toShort()
                        equip.upgradeSlots = it[InventoryEquipment.upgradableSlots].toByte()
                        equip.level = it[InventoryEquipment.level].toByte()
                        equip.itemExp = it[InventoryEquipment.itemExp].toFloat()
                        equip.itemLevel = it[InventoryEquipment.itemLevel].toByte()
                        equip.expiration = it[InventoryItems.expiration]
                        equip.giftFrom = it[InventoryItems.giftFrom]
                        equip.ringId = it[InventoryEquipment.ringId]
                        items.add(Pair(equip, mit))
                    } else {
                        val item = Item(it[InventoryItems.itemId], it[InventoryItems.position].toByte(),
                            it[InventoryItems.quantity].toShort(), it[InventoryItems.petId])
                        item.owner = it[InventoryItems.owner]
                        item.expiration = it[InventoryItems.expiration]
                        item.giftFrom = it[InventoryItems.giftFrom]
                        item.flag = it[InventoryItems.flag]
                        items.add(Pair(item, mit))
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to load items from database. Id: $id" }
        }
        return items
    }

    /**
     * Synchronized function to save items to the database.
     *
     * This function takes a list of items and an ID as parameters. It then saves these items to the database.
     * It first deletes any existing items associated with the given ID and item factory type from the database.
     * It then iterates over the list of items and inserts each item into the database.
     * If the item is of type EQUIPPED or EQUIP, it also inserts the item into the InventoryEquipment table.
     *
     * @param items The list of items to save to the database. Each item is a pair consisting of an Item object and an InventoryType object.
     * @param id The ID associated with the items. This can be either an account ID or a character ID, depending on the type of item factory.
     * @throws SQLException If an SQL error occurs while saving the items to the database.
     * @see Item for the class that represents an item.
     * @see InventoryType for the enum class that represents the type of item.
     * @see ItemFactory for the enum class that represents the type of item factory.
     * @see ItemFactory.loadItems for the method that loads items from the item factory.
     */
    @Synchronized fun saveItems(items: List<Pair<Item, InventoryType>>, id: Int) {
        try {
            transaction {
                InventoryItems.deleteWhere {
                    (type eq value.toByte()) and
                            if (account) (accountId eq id)
                            else (characterId eq id)
                }
                items.forEach { (item, type) ->
                    val ii = InventoryItems.insert {
                        it[InventoryItems.type] = value.toByte()
                        it[accountId] = if (account) id else -1
                        it[characterId] = if (account) -1 else id
                        it[itemId] = item.itemId
                        it[inventoryType] = type.type.toInt()
                        it[position] = item.position.toInt()
                        it[quantity] = item.quantity.toInt()
                        it[owner] = item.owner
                        it[petId] = item.petId
                        it[flag] = item.flag
                        it[expiration] = item.expiration
                        it[giftFrom] = item.giftFrom
                    }
                    val row = ii.resultedValues?.first()
                    if ((type == InventoryType.EQUIPPED || type == InventoryType.EQUIP) && row != null) {
                        item as Equip
                        InventoryEquipment.insert {
                            it[inventoryItemId] = row[InventoryItems.id]
                            it[upgradableSlots] = item.upgradeSlots.toInt()
                            it[str] = item.str.toInt()
                            it[dex] = item.dex.toInt()
                            it[int] = item.int.toInt()
                            it[luk] = item.luk.toInt()
                            it[level] = item.level.toInt()
                            it[hp] = item.hp.toInt()
                            it[mp] = item.mp.toInt()
                            it[watk] = item.watk.toInt()
                            it[matk] = item.matk.toInt()
                            it[wdef] = item.wdef.toInt()
                            it[mdef] = item.mdef.toInt()
                            it[acc] = item.acc.toInt()
                            it[avoid] = item.avoid.toInt()
                            it[hands] = item.hands.toInt()
                            it[speed] = item.speed.toInt()
                            it[jump] = item.jump.toInt()
                            it[locked] = 0
                            it[vicious] = item.vicious.toInt()
                            it[itemLevel] = item.itemLevel.toInt()
                            it[itemExp] = item.itemExp
                            it[ringId] = item.ringId
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save items to database. Id: $id" }
        }
    }

    companion object : KLogging()
}