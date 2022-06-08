package server

import client.Character
import client.Client
import server.maps.AbstractMapObject
import server.maps.MapObjectType
import tools.packet.CharacterPacket
import tools.packet.InteractPacket

class PlayerShop(val owner: Character, val description: String) : AbstractMapObject() {
    val visitors = arrayOfNulls<Character>(3)
    private val slots = arrayOfNulls<Character>(3)
    val items = mutableListOf<PlayerShopItem>()
    private val bannedList = mutableListOf<String>()
    private var boughtNumber = 0

    init {
        position = owner.position
    }

    fun hasFreeSlot(): Boolean = visitors.contains(null)

    fun isOwner(c: Character?) = owner == c

    fun addVisitor(visitor: Character) {
        val lastIndex = visitors.indexOfLast { it != null } + 1
        visitors[lastIndex] = visitor
        val lastSlot = slots.indexOfLast { it != null } + 1
        visitor.slots = lastSlot
        slots[lastSlot] = visitor
        if (lastSlot == 2) {
            visitor.map.broadcastMessage(CharacterPacket.addCharBox(owner, 1))
        }
    }

    fun removeVisitor(visitor: Character) {
        if (visitor == owner) {
            owner.map.removeMapObject(this)
            owner.playerShop = null
        }
        val index = visitors.indexOfLast { it != null && it.id == visitor.id }
        val slot = visitor.slots
        visitors[index] = null
        slots[index] = null
        visitor.slots = -1
        this.broadcast(InteractPacket.getPlayerShopRemoveVisitor(slot + 1))
    }

    fun removeVisitors() {
        visitors.forEach {
            it?.client?.announce(InteractPacket.shopErrorMessage(10, 1))
            it?.let { removeVisitor(it) }
        }
    }

    fun isVisitor(visitor: Character?) = visitors.contains(visitor)

    fun addItem(item: PlayerShopItem) = items.add(item)

    fun removeItem(item: Int) = items.removeAt(item)

    fun buy(c: Client, item: Int, quantity: Short) {
        if (isVisitor(c.player)) {
            val pItem = items[item]
            val newItem = pItem.item.copy()
            newItem.quantity = newItem.quantity
            if (quantity < 1 || pItem.bundles <1 || newItem.quantity > pItem.bundles || !pItem.doesExist) return
            else if (newItem.getType() == 1 && newItem.quantity > 1) return
            c.player?.let { player ->
                synchronized(player) {
                    if (InventoryManipulator.addFromDrop(c, newItem, false)) {
                        if (player.meso.get() >= pItem.price * quantity) {
                            player.gainMeso(-pItem.price * quantity, true)
                            owner.gainMeso(pItem.price * quantity, true)
                            pItem.bundles = (pItem.bundles - quantity).toShort()
                            if (pItem.bundles < 1) {
                                pItem.doesExist = false
                                if (++boughtNumber == items.size) {
                                    owner.playerShop = null
                                    owner.map.broadcastMessage(CharacterPacket.removeCharBox(owner))
                                    removeVisitors()
                                    owner.dropMessage(1, "모든 물건이 다 팔렸습니다.")
                                }
                            }
                        } else {
                            player.dropMessage(1, "메소가 부족 합니다.")
                        }
                    } else {
                        player.dropMessage(1, "인벤토리가 가득 찼습니다.")
                    }
                }
            }
        }
    }

    fun chat(c: Client, chat: String) {
        var s = 0
        visitors.forEach { visitor ->
            s++
            if (visitor != null) {
                if (visitor.name.lowercase() == c.player?.name?.lowercase()) return@forEach
            }
            if (s == 3) s = 0
        }
        broadcast(InteractPacket.getPlayerShopChat(c.player, chat, s.toByte()))
    }

    fun sendShop(c: Client) = c.announce(InteractPacket.getPlayerShop(c, this, isOwner(c.player)))

    fun broadcast(packet: ByteArray) {
        if (owner.client.session != null) {
            owner.client.announce(packet)
        }
        broadcastToVisitors(packet)
    }

    private fun broadcastToVisitors(packet: ByteArray) {
        visitors.forEach { visitor ->
            visitor?.client?.announce(packet)
        }
    }

    fun banPlayer(name: String) {
        if (!bannedList.contains(name)) {
            bannedList.add(name)
        }
        visitors.forEach {
            if (it != null && it.name == name) {
                it.client.announce(InteractPacket.shopErrorMessage(5, 1))
                removeVisitor(it)
                return@forEach
            }
        }
    }

    fun isBanned(name: String) = bannedList.contains(name)

    override val objectType = MapObjectType.SHOP

    override fun sendDestroyData(client: Client) {
        client.announce(CharacterPacket.removeCharBox(owner))
    }

    override fun sendSpawnData(client: Client) {
        client.announce(CharacterPacket.addCharBox(owner, 4))
    }
}