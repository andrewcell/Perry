package client.inventory

import database.InventoryEquipment
import database.InventoryItems
import mu.KLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.SQLException

enum class ItemFactory(val value: Int, val account: Boolean) {
    INVENTORY(1, false),
    STORAGE(2, true),
    CASH_EXPLORER(3, true),
    MERCHANT(6, false);

    fun loadItems(id: Int, login: Boolean): List<Pair<Item, InventoryType>> {
        val items = mutableListOf<Pair<Item, InventoryType>>()
        try {
            transaction {
                var rs = (InventoryItems leftJoin InventoryEquipment).select {
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

    @Synchronized fun saveItems(items: List<Pair<Item, InventoryType>>, id: Int) {
        try {
            transaction {
                InventoryItems.deleteWhere {
                    (InventoryItems.type eq value.toByte()) and
                            if (account) (InventoryItems.accountId eq id)
                            else (InventoryItems.characterId eq id)
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
                        it[flag] = item.flag.toInt()
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