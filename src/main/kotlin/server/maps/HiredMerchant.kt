package server.maps

import client.Character
import client.Client
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.ItemFactory
import constants.ItemConstants
import database.Characters
import kotlinx.coroutines.Job
import mu.KLoggable
import net.server.Server
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import server.InventoryManipulator
import server.ItemInformationProvider
import server.PlayerShopItem
import tools.CoroutineManager
import tools.PacketCreator
import tools.packet.CashPacket
import java.awt.Point
import java.sql.SQLException

class HiredMerchant(owner: Character, val itemId: Int, val description: String) : AbstractMapObject(), KLoggable {
    override val logger = logger()
    override var position: Point = owner.position
    val start = System.currentTimeMillis()
    val ownerId = owner.id
    val channel = owner.client.channel
    val world = owner.world
    val ownerName = owner.name
    var map = owner.map
    var schedule: Job? = CoroutineManager.schedule({
        this.closeShop(owner.client, true)
    }, (1000 * 60 * 60 * 24).toLong())

    val items = mutableListOf<PlayerShopItem>()
    val visitors = arrayOfNulls<Character>(3)
    val messages = mutableListOf<Pair<String, Byte>>()
    private val sold = mutableListOf<SoldItem>()
    var open = false

    fun broadcastToVisitors(packet: ByteArray) = visitors.forEach { it?.client?.announce(packet) }

    fun addVisitor(visitor: Character) {
        val index = getFreeSlot()
        if (index > -1) {
            visitors[index] = visitor
            broadcastToVisitors(CashPacket.hiredMerchantVisitorAdd(visitor, index + 1))
        }
    }

    fun removeVisitor(visitor: Character)  {
        val slot = getVisitorSlot(visitor)
        if (visitors[slot] == visitor) {
            visitors[slot] = null
            if (slot != -1) broadcastToVisitors(CashPacket.hiredMerchantVisitorLeave(slot + 1))
        }
    }

    fun removeAllVisitors(message: String) {
        visitors.forEachIndexed { i, v ->
            if (v != null) {
                v.hiredMerchant = null
                v.client.announce(CashPacket.leaveHiredMerchant(i + 1, 0x11))
                if (message.isNotEmpty()) v.dropMessage(1, message)
                visitors[i] = null
            }
        }
    }

    fun getVisitorSlot(visitor: Character) = visitors.indexOf(visitor)

    fun getFreeSlot() = visitors.indexOfFirst { it == null }

    fun buy(c: Client, item: Int, quantity: Int) {
        c.player?.let { player ->
            val pItem = items[item]
            synchronized(items) {
                val newItem = pItem.item.copy()
                newItem.quantity = (pItem.item.quantity * quantity).toShort()
                if ((newItem.flag.and(ItemConstants.KARMA) == ItemConstants.KARMA))
                    newItem.flag = newItem.flag xor ItemConstants.KARMA
                if (newItem.getType() == 2 && (newItem.flag and ItemConstants.SPIKES) == ItemConstants.SPIKES)
                    newItem.flag = newItem.flag xor ItemConstants.SPIKES
                if ((quantity < 1 || pItem.bundles < 1 || !pItem.doesExist || pItem.bundles < quantity) ||
                    (newItem.getType() == 1 && newItem.quantity > 1) || !pItem.doesExist) {
                        c.announce(PacketCreator.enableActions())
                        return
                }
                val price = pItem.price * quantity
                if (player.meso.get() >= price) {
                    if (InventoryManipulator.addFromDrop(c, newItem, true)) {
                        player.gainMeso(-price, false)
                        sold.add(SoldItem(player.name, pItem.item.itemId, quantity, price))
                        pItem.bundles = (pItem.bundles - quantity).toShort()
                        if (pItem.bundles < 1) pItem.doesExist = false
                        val owner = Server.getWorld(world).players.getCharacterByName(ownerName)
                        if (owner != null) {
                            owner.addMerchantMesos(price)
                        } else {
                            try {
                                transaction {
                                    val chr = Characters.select(Characters.merchantMesos).where { Characters.id eq ownerId }
                                    Characters.update({ Characters.id eq ownerId }) {
                                        it[merchantMesos] = chr.first()[merchantMesos] + price
                                    }
                                }
                            } catch (e: SQLException) {
                                logger.error(e) { "Failed to update merchant mesos in database. CharacterId: $ownerId" }
                            }
                        }
                    } else {
                        player.dropMessage(1, "인벤토리가 꽉 찼습니다.")
                    }
                } else {
                    player.dropMessage(1, "메소가 부족합니다.")
                }
                saveItems(false)
            }
        }
    }

    fun closeShop(c: Client, timeout: Boolean) {
        map.removeMapObject(this)
        map.broadcastMessage(CashPacket.destroyHiredMerchant(ownerId))
        c.getChannelServer().removeHiredMerchant(ownerId)
        try {
            transaction {
                Characters.update({ Characters.id eq ownerId }) {
                    it[hasMerchant] = false
                }
            }

            if (checkMerchant(c.player, items) && !timeout) {
                items.forEach {
                    if (it.doesExist && (it.item.getType() == 1)) {
                        InventoryManipulator.addFromDrop(c, it.item, false)
                    } else if (it.doesExist) {
                        InventoryManipulator.addById(
                            c,
                            it.item.itemId,
                            (it.bundles * it.item.quantity).toShort(),
                            null,
                            -1,
                            it.item.expiration
                        )
                    }
                }
                items.clear()
            }
            saveItems(false)
            items.clear()
        } catch (e: Exception) {
            logger.error(e) { "Error caused when close merchant. CharacterId: $ownerId" }
        }
        schedule?.cancel()
    }

    fun forceClose() {
        schedule?.cancel()
        saveItems(true)
        Server.getChannel(world, channel).removeHiredMerchant(ownerId)
        map.broadcastMessage(CashPacket.destroyHiredMerchant(ownerId))
        map.removeMapObject(this)
        schedule = null
    }

    fun addItem(item: PlayerShopItem) {
        items.add(item)
        saveItems(false)
    }

    fun removeFromSlot(slot: Int) {
        items.removeAt(slot)
        saveItems(false)
    }

    fun saveItems(shutdown: Boolean) {
        val itemsWithType = mutableListOf<Pair<Item, InventoryType>>()
        items.forEach {
            val newItem = it.item
            if (shutdown) {
                newItem.quantity = (it.item.quantity * it.bundles).toShort()
            } else {
                newItem.quantity = it.item.quantity
            }
            if (it.bundles > 0) {
                itemsWithType.add(Pair(newItem, InventoryType.getByType(newItem.getType().toByte())))
            }
        }
        ItemFactory.MERCHANT.saveItems(itemsWithType, ownerId)
    }

    fun isOwner(chr: Character) = chr.id == ownerId

    fun getTimeLeft() = ((System.currentTimeMillis() - start) / 1000).toInt()

    override val objectType: MapObjectType = MapObjectType.HIRED_MERCHANT

    override fun sendDestroyData(client: Client) { }

    override fun sendSpawnData(client: Client) = client.announce(CashPacket.spawnHiredMerchant(this))

    data class SoldItem(val buyer: String, val itemId: Int, val quantity: Int, val mesos: Int)

    companion object {
        fun checkMerchant(chr: Character?, items: List<PlayerShopItem>): Boolean {
            if (chr == null) return false
            var eq = 0
            var use = 0
            var setup = 0
            var etc = 0
            var cash = 0
            val list = mutableListOf<InventoryType>()
            items.forEach {
                val invType = ItemInformationProvider.getInventoryType(it.item.itemId)
                if (!list.contains(invType)) list.add(invType)
                when (invType) {
                    InventoryType.EQUIP -> eq++
                    InventoryType.USE -> use++
                    InventoryType.SETUP -> setup++
                    InventoryType.ETC -> etc++
                    InventoryType.CASH -> cash++
                    else -> eq += 0
                }
            }
            list.forEach {
                when (it) {
                    InventoryType.EQUIP -> if ((chr.getInventory(InventoryType.EQUIP)?.getNumFreeSlot() ?: 0) <= eq) return false
                    InventoryType.USE -> if ((chr.getInventory(InventoryType.USE)?.getNumFreeSlot() ?: 0) <= use) return false
                    InventoryType.SETUP -> if ((chr.getInventory(InventoryType.SETUP)?.getNumFreeSlot() ?: 0) <= setup) return false
                    InventoryType.ETC -> if ((chr.getInventory(InventoryType.ETC)?.getNumFreeSlot() ?: 0) <= etc) return false
                    InventoryType.CASH -> if ((chr.getInventory(InventoryType.CASH)?.getNumFreeSlot() ?: 0) <= cash) return false
                    else -> return true
                }
            }
            return true
        }
    }
}