/**
 * This package contains the database related classes and objects.
 */
package database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

/**
 * This object represents the InventoryEquipment table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object InventoryEquipment : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the inventory item ID associated with the equipment.
     * It is an integer and references the id column in the InventoryItems table.
     * The reference option is set to CASCADE.
     */
    val inventoryItemId = integer("inventoryItemId").references(InventoryItems.id, ReferenceOption.CASCADE)

    /**
     * This column represents the upgradable slots of the equipment.
     * It is an integer with a default value of 0.
     */
    val upgradableSlots = integer("upgradableSLots").default(0)

    /**
     * This column represents the level of the equipment.
     * It is an integer with a default value of 0.
     */
    val level = integer("level").default(0)

    /**
     * This column represents the strength of the equipment.
     * It is an integer with a default value of 0.
     */
    val str = integer("str").default(0)

    /**
     * This column represents the dexterity of the equipment.
     * It is an integer with a default value of 0.
     */
    val dex = integer("dex").default(0)

    /**
     * This column represents the intelligence of the equipment.
     * It is an integer with a default value of 0.
     */
    val int = integer("int").default(0)

    /**
     * This column represents the luck of the equipment.
     * It is an integer with a default value of 0.
     */
    val luk = integer("luk").default(0)

    /**
     * This column represents the health points of the equipment.
     * It is an integer with a default value of 0.
     */
    val hp = integer("hp").default(0)

    /**
     * This column represents the magic points of the equipment.
     * It is an integer with a default value of 0.
     */
    val mp = integer("mp").default(0)

    /**
     * This column represents the weapon attack of the equipment.
     * It is an integer with a default value of 0.
     */
    val watk = integer("watk").default(0)

    /**
     * This column represents the magic attack of the equipment.
     * It is an integer with a default value of 0.
     */
    val matk = integer("matk").default(0)

    /**
     * This column represents the weapon defense of the equipment.
     * It is an integer with a default value of 0.
     */
    val wdef = integer("wdef").default(0)

    /**
     * This column represents the magic defense of the equipment.
     * It is an integer with a default value of 0.
     */
    val mdef = integer("mdef").default(0)

    /**
     * This column represents the accuracy of the equipment.
     * It is an integer with a default value of 0.
     */
    val acc = integer("acc").default(0)

    /**
     * This column represents the avoidance of the equipment.
     * It is an integer with a default value of 0.
     */
    val avoid = integer("avoid").default(0)

    /**
     * This column represents the hands of the equipment.
     * It is an integer with a default value of 0.
     */
    val hands = integer("hands").default(0)

    /**
     * This column represents the speed of the equipment.
     * It is an integer with a default value of 0.
     */
    val speed = integer("speed").default(0)

    /**
     * This column represents the jump of the equipment.
     * It is an integer with a default value of 0.
     */
    val jump = integer("jump").default(0)

    /**
     * This column represents the locked status of the equipment.
     * It is an integer.
     */
    val locked = integer("locked")

    /**
     * This column represents the vicious status of the equipment.
     * It is an integer with a default value of 0.
     */
    val vicious = integer("vicious").default(0)

    /**
     * This column represents the item level of the equipment.
     * It is an integer with a default value of 1.
     */
    val itemLevel = integer("itemLevel").default(1)

    /**
     * This column represents the item experience points of the equipment.
     * It is a float with a default value of 0.
     */
    val itemExp = float("itemExp").default(0f)

    /**
     * This column represents the ring ID of the equipment.
     * It is an integer with a default value of -1.
     */
    val ringId = integer("ringId").default(-1)

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}