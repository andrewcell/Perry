package client.inventory

import kotlin.random.Random

open class Item(val itemId: Int, var position: Byte, open var quantity: Short, val petId: Int = -1) : Comparable<Item> {
    var sn = -1
    var cashId = Random(128).nextInt(Integer.MAX_VALUE)
        //get() = if (field == 0) Random(128).nextInt(Integer.MAX_VALUE) + 1 else field
    var log = mutableListOf<String>()
    val pet = if (petId > -1) Pet.loadFromDatabase(itemId, position, petId) else null
    open var flag = 0
    var owner = ""
    var giftFrom = ""
    var expiration = -1L

    open fun copy(): Item {
        val ret = Item(itemId, position, quantity, petId)
        ret.flag = flag
        ret.owner = owner
        ret.expiration = expiration
        ret.log = log.toMutableList()
        return ret
    }

    open fun getType() = if (petId > -1) 3 else 2

    override fun compareTo(other: Item): Int {
        return if (this.itemId < other.itemId) -1
        else if (this.itemId > other.itemId) 1
        else 0
    }
}