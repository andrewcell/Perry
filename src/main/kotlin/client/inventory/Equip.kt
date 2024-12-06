package client.inventory

import client.Client
import server.ItemInformationProvider
import tools.packet.CharacterPacket
import tools.packet.ItemPacket

/**
 * This class represents an equipable item in the game.
 *
 * The Equip class extends the Item class and adds additional properties and methods that are specific to equipable items.
 * These include properties for the item's stats (such as STR, DEX, INT, and LUK), upgrade slots, and whether the item is currently being worn.
 * It also includes methods to gain experience and level up the item, and to copy the item.
 *
 * @property id The unique identifier of the item.
 * @property position The position of the item in the inventory.
 * @property upgradeSlots The number of slots available for upgrading the item. Defaults to -1, which represents an unlimited number of upgrade slots.
 * @constructor Creates a new instance of the Equip class with the specified id, position, and number of upgrade slots.
 * @see Item for the base class that Equip extends.
 */
class Equip(id: Int, position: Byte, var upgradeSlots: Byte = -1) : Item(id, position, 1) {
    /**
     * Enum class representing the possible results of a scroll operation on an equipable item.
     *
     * The ScrollResult enum has three possible values: FAIL, SUCCESS, and CURSE. Each value is associated with an integer.
     * FAIL (0) represents a failed scroll operation, where the item is not upgraded.
     * SUCCESS (1) represents a successful scroll operation, where the item is upgraded.
     * CURSE (2) represents a cursed scroll operation, where the item is downgraded or destroyed.
     *
     * @property value The integer associated with the scroll result.
     * @see Equip for the class that uses this enum.
     */
    enum class ScrollResult(val value: Int) {
        FAIL(0), SUCCESS(1), CURSE(2);
    }

    /**
     * The experience of the item. This is a floating point value that represents the current experience of the item.
     * The experience is used to level up the item.
     */
    var itemExp: Float = 0F

    /**
     * The level of the item. This is a byte value that represents the current level of the item.
     * The level is increased when the item gains enough experience.
     */
    var itemLevel: Byte = 1

    /**
     * A boolean value that indicates whether the item is currently being worn by a character.
     */
    var isWearing = false

    /**
     * The level of the item. This is a byte value that represents the current level of the item.
     * The level is increased when the item gains enough experience.
     */
    var level: Byte = 0

    /**
     * The strength stat of the item. This is a short value that represents the strength stat of the item.
     */
    var str: Short = 0

    /**
     * The dexterity stat of the item. This is a short value that represents the dexterity stat of the item.
     */
    var dex: Short = 0

    /**
     * The intelligence stat of the item. This is a short value that represents the intelligence stat of the item.
     */
    var int: Short = 0

    /**
     * The luck stat of the item. This is a short value that represents the luck stat of the item.
     */
    var luk: Short = 0

    /**
     * The health points stat of the item. This is a short value that represents the health points stat of the item.
     */
    var hp: Short = 0

    /**
     * The mana points stat of the item. This is a short value that represents the mana points stat of the item.
     */
    var mp: Short = 0

    /**
     * The weapon attack stat of the item. This is a short value that represents the weapon attack stat of the item.
     */
    var watk: Short = 0

    /**
     * The magic attack stat of the item. This is a short value that represents the magic attack stat of the item.
     */
    var matk: Short = 0

    /**
     * The weapon defense stat of the item. This is a short value that represents the weapon defense stat of the item.
     */
    var wdef: Short = 0

    /**
     * The magic defense stat of the item. This is a short value that represents the magic defense stat of the item.
     */
    var mdef: Short = 0

    /**
     * The accuracy stat of the item. This is a short value that represents the accuracy stat of the item.
     */
    var acc: Short = 0

    /**
     * The avoidability stat of the item. This is a short value that represents the avoidability stat of the item.
     */
    var avoid: Short = 0

    /**
     * The hands stat of the item. This is a short value that represents the hands stat of the item.
     */
    var hands: Short = 0

    /**
     * The speed stat of the item. This is a short value that represents the speed stat of the item.
     */
    var speed: Short = 0

    /**
     * The jump stat of the item. This is a short value that represents the jump stat of the item.
     */
    var jump: Short = 0

    /**
     * The vicious stat of the item. This is a short value that represents the vicious stat of the item.
     */
    var vicious: Short = 0

    /**
     * The ring ID of the item. This is an integer value that represents the ring ID of the item.
     * The default value is -1, which indicates that the item is not a ring.
     */
    var ringId = -1

    /**
     * Creates a copy of the current Equip object.
     *
     * This function creates a new instance of the Equip class with the same properties as the current object.
     * It then returns this new object. This is useful for duplicating items in the game.
     *
     * @return A new Equip object with the same properties as the current object.
     * @see Equip for the class that this method is a part of.
     */
    override fun copy(): Item {
        val ret = Equip(itemId, position, upgradeSlots)
        ret.str = str
        ret.dex = dex
        ret.int = int
        ret.luk = luk
        ret.hp = hp
        ret.mp = mp
        ret.matk = matk
        ret.mdef = mdef
        ret.watk = watk
        ret.wdef = wdef
        ret.acc = acc
        ret.avoid = avoid
        ret.hands = hands
        ret.speed = speed
        ret.jump = jump
        ret.flag = flag
        ret.vicious = vicious
        ret.upgradeSlots = upgradeSlots
        ret.itemLevel = itemLevel
        ret.itemExp = itemExp
        ret.level = level
        ret.log = log
        ret.owner = owner
        ret.quantity = quantity
        ret.expiration = expiration
        ret.giftFrom = giftFrom
        return ret
    }

    /**
     * Increases the level of the item.
     *
     * This function is used to increase the level of the item. It first retrieves the stats for leveling up the item from the ItemInformationProvider.
     * It then iterates over these stats and increases the corresponding stat of the item by the value of the stat from the level up stats.
     * After all stats have been increased, the level of the item is incremented.
     *
     * Finally, it sends a packet to the client to show that the item has leveled up and broadcasts a message to all other players in the same map to show that the item has leveled up.
     *
     * @param c The client that the item belongs to.
     * @param timeless A boolean value that indicates whether the item is timeless.
     * @see Client for the definition of a client.
     * @see ItemInformationProvider for the provider that gives the stats for leveling up the item.
     * @see ItemInformationProvider.getItemLevelUpStats for the method that retrieves the stats for leveling up the item.
     * @see Client.announce for the method that sends a packet to the client.
     * @see ItemPacket.showEquipmentLevelUp for the packet that is sent to the client to show that the item has leveled up.
     * @see Character for the definition of a player.
     * @see Character.map for the map that the player is currently in.
     * @see Map.broadcastMessage for the method that sends a message to all players in the same map.
     * @see CharacterPacket.showForeignEffect for the message that is sent to all other players in the same map to show that the item has leveled up.
     * @see Player.forceUpdateItem for the method that updates the item in the player's inventory.
     */
    private fun gainLevel(c: Client, timeless: Boolean) {
        val stats = ItemInformationProvider.getItemLevelUpStats(itemId, itemLevel.toInt(), timeless)
        stats.forEach { (name, value) ->
            val v = value.toShort()
            when (name) {
                "incDEX" -> dex = (dex + v).toShort()
                "incSTR" -> str = (str + value).toShort()
                "incINT" -> int = (int + value).toShort()
                "incLUK" -> luk = (luk + value).toShort()
                "incMHP" -> hp = (hp + value).toShort()
                "incMMP" -> mp = (mp + value).toShort()
                "incPAD" -> watk = (watk + value).toShort()
                "incMAD" -> matk = (matk + value).toShort()
                "incPDD" -> wdef = (wdef + value).toShort()
                "incMDD" -> mdef = (mdef + value).toShort()
                "incEVA" -> avoid = (avoid + value).toShort()
                "incACC" -> acc = (acc + value).toShort()
                "incSpeed" -> speed = (speed + value).toShort()
                "incJump" -> jump = (jump + value).toShort()
            }
        }
        itemLevel++
        c.announce(ItemPacket.showEquipmentLevelUp())
        c.player?.let {
            it.map.broadcastMessage(it, CharacterPacket.showForeignEffect(it.id, 15))
            it.forceUpdateItem(this)
        }
    }

    fun gainItemExp(c: Client, gain: Int, timeless: Boolean) {
        val expNeeded = if (timeless) 10 * itemLevel + 70 else 5 * itemLevel + 65
        val modifier = 364.0 / expNeeded
        val exp = (expNeeded / (1000000 * modifier * modifier)) * gain
        itemExp += exp.toFloat()
        if (itemExp >= 364) {
            itemExp -= 364
            gainLevel(c, timeless)
        } else {
            c.player?.forceUpdateItem(this)
        }
    }

    /**
     * The quantity of the item. This is a short value that represents the quantity of the item.
     *
     * The getter returns the quantity of the item from the superclass.
     *
     * The setter sets the quantity of the item in the superclass if the provided value is not less than 0 and the current quantity is not greater than 1.
     * If the provided value is less than 0 or the current quantity is greater than 1, the setter does nothing and returns.
     *
     * @property value The new quantity of the item.
     * @see Item for the superclass that this property is a part of.
     * @see Item.quantity for the quantity property in the superclass.
     */
    override var quantity: Short
        get() = super.quantity
        set(value) {
            if (value < 0 || quantity > 1) return
            super.quantity = value
        }

    /**
     * Returns the type of the item.
     *
     * @return 1, which represents an equipable item.
     * @see Item for the superclass that this method is a part of.
     * @see Item.getType for the method in the superclass that this method overrides.
     */
    override fun getType() = 1
}