package client.inventory

import kotlin.random.Random

/**
 * Represents an item in the game.
 *
 * This class is used to represent an item in the game. It contains properties for the item's ID, position, quantity, and pet ID.
 * It also contains properties for the item's serial number, cash ID, log, pet, flag, owner, gift from, and expiration.
 * It also contains methods for copying the item, getting the type of the item, and comparing the item to another item.
 *
 * @property itemId The ID of the item.
 * @property position The position of the item.
 * @property quantity The quantity of the item.
 * @property petId The ID of the pet associated with the item.
 * @property sn The serial number of the item.
 * @property cashId The cash ID of the item.
 * @property log The log of the item.
 * @property pet The pet associated with the item.
 * @property flag The flag of the item.
 * @property owner The owner of the item.
 * @property giftFrom The person who gifted the item.
 * @property expiration The expiration date of the item.
 * @constructor Creates a new item with the given ID, position, quantity, and pet ID.
 * @see Pet for the class that represents a pet.
 * @see Pet.loadFromDatabase for the method that loads a pet from the database.
 * @see Item.copy for the method that copies the item.
 * @see Item.getType for the method that gets the type of the item.
 * @see Item.compareTo for the method that compares the item to another item.
 */
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


    /**
     * Creates a copy of the current Item object.
     *
     * This function creates a new instance of the Item class with the same properties as the current object.
     * It then returns this new object. This is useful for duplicating items in the game.
     *
     * @return A new Item object with the same properties as the current object.
     * @see Item for the class that this method is a part of.
     */
    open fun copy(): Item {
        val ret = Item(itemId, position, quantity, petId)
        ret.flag = flag
        ret.owner = owner
        ret.expiration = expiration
        ret.log = log.toMutableList()
        return ret
    }

    /**
     * Returns the type of the item.
     *
     * This function is used to get the type of the item. It returns 3 if the item is associated with a pet, and 2 otherwise.
     * This is used to differentiate items associated with pets from other items in the game.
     *
     * @return 3 if the item is associated with a pet, and 2 otherwise.
     * @see Item for the class that this method is a part of.
     */
    open fun getType() = if (petId > -1) 3 else 2


    /**
     * Compares the current item to another item.
     *
     * This function is used to compare the current item to another item. It compares the items based on their IDs.
     * It returns -1 if the ID of the current item is less than the ID of the other item, 1 if the ID of the current item is greater than the ID of the other item, and 0 otherwise.
     *
     * @param other The other item to compare to.
     * @return -1 if the ID of the current item is less than the ID of the other item, 1 if the ID of the current item is greater than the ID of the other item, and 0 otherwise.
     * @see Item for the class that this method is a part of.
     */
    override fun compareTo(other: Item): Int {
        return if (this.itemId < other.itemId) -1
        else if (this.itemId > other.itemId) 1
        else 0
    }
}