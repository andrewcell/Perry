package server

import client.Client
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.ItemFactory
import database.Storages
import mu.KLogging
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import tools.packet.CharacterPacket
import java.sql.SQLException

class Storage(val id: Int, var slots: Byte, var meso: Int) {
    val items = mutableListOf<Item>()
    private val typeItems = mutableMapOf<InventoryType, List<Item>>()

    fun gainSlots(slots: Int): Boolean {
        var tempSlots = slots
        tempSlots += this.slots
        if (tempSlots <= 48) {
            this.slots = slots.toByte()
            return true
        }
        return false
    }

    fun saveToDatabase() {
        try {
            val slot = slots
            val mesoValue = meso
            val sid = id
            transaction {
                Storages.update ({ Storages.id eq sid }) {
                    it[slots] = slot.toInt()
                    it[meso] = mesoValue
                }
            }
            val itemsWithType = mutableListOf<Pair<Item, InventoryType>>()
            items.forEach { item ->
                itemsWithType.add(Pair(item, ItemInformationProvider.getInventoryType(item.itemId)))
            }
            ItemFactory.STORAGE.saveItems(itemsWithType, id)
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save storage data to database. StorageId: $id" }
        }
    }

    fun getItem(slot: Byte) = items[slot.toInt()]

    fun takeOut(slot: Byte): Item {
        val ret = items.removeAt(slot.toInt())
        val type = ItemInformationProvider.getInventoryType(ret.itemId)
        typeItems[type] = filterItems(type)
        return ret
    }

    fun store(item: Item) {
        items.add(item)
        val type = ItemInformationProvider.getInventoryType(item.itemId)
        typeItems[type] = filterItems(type)
    }

    private fun filterItems(type: InventoryType): List<Item> {
        val ret = mutableListOf<Item>()
        val ii = ItemInformationProvider
        items.forEach { item ->
            if (ii.getInventoryType(item.itemId) == type) {
                ret.add(item)
            }
        }
        return ret.toList()
    }

    fun getSlot(type: InventoryType, slot: Byte): Byte {
        var ret: Byte = 0
        items.forEach { item ->
            if (item == typeItems[type]?.get(slot.toInt())) return ret
            ret++
        }
        return -1
    }

    fun sendStorage(c: Client, npcId: Int) {
        val ii = ItemInformationProvider
        items.sortWith { o1, o2 ->
            if (ii.getInventoryType(o1.itemId).type < ii.getInventoryType(o2.itemId).type ) {
                return@sortWith -1
            } else if (ii.getInventoryType(o1.itemId) == ii.getInventoryType(o2.itemId)) {
                return@sortWith 0
            }
            1
        }
        InventoryType.values().forEach { type ->
            typeItems[type] = items
        }
        c.announce(CharacterPacket.getStorage(npcId, slots, items, meso))
    }

    fun sendStored(c: Client, type: InventoryType) {
        c.announce(CharacterPacket.storeStorage(slots, type, typeItems[type] ?: emptyList()))
    }

    fun sendTakenOut(c: Client, type: InventoryType) {
        c.announce(CharacterPacket.takeOutStorage(slots, type, typeItems[type] ?: emptyList()))
    }

    fun sendMeso(c: Client) = c.announce(CharacterPacket.mesoStorage(slots, meso))

    fun isFull() = items.size >= slots

    fun close() = typeItems.clear()

    companion object : KLogging() {
        fun loadOrCreateFromDb(id: Int, world: Int): Storage? {
            var storeId: Int
            var ret: Storage? = null
            try {
                transaction {
                    val rs = Storages.select { (Storages.accountId eq id) and (Storages.world eq world) }
                    if (rs.empty()) ret = create(id, world)
                    else {
                        val storage = rs.first()
                        storeId = storage[Storages.id]
                        ret = Storage(storeId, storage[Storages.slots].toByte(), storage[Storages.meso])
                        ret?.let {
                            ItemFactory.STORAGE.loadItems(it.id, false).forEach { item ->
                                it.items.add(item.first)
                            }
                        }

                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to load storage data from database." }
            }
            return ret
        }

        fun create(id: Int, world: Int): Storage? {
            try {
                transaction {
                    Storages.insert {
                        it[accountId] = id
                        it[Storages.world] = world
                        it[slots] = 4
                        it[meso] = 0
                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to create storage data to database." }
            }
            return loadOrCreateFromDb(id, world)
        }
    }
}