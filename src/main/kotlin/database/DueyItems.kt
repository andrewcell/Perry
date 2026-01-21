/**
 * This package contains the database related classes and objects.
 */
package database

import org.jetbrains.exposed.v1.core.Table

/**
 * This object represents the DueyItems table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object DueyItems : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the package ID associated with the duey item.
     * It is an integer and references the packageId column in the DueyPackages table.
     */
    val packageId = integer("packageId").references(DueyPackages.packageId)

    /**
     * This column represents the item ID of the duey item.
     * It is an integer.
     */
    val itemId = integer("itemId")

    /**
     * This column represents the quantity of the duey item.
     * It is an integer.
     */
    val quantity = integer("quantity")

    /**
     * This column represents the upgrade slots of the duey item.
     * It is an integer with a default value of 0.
     */
    val upgradeSlots = integer("upgradeSlots").default(0)

    /**
     * This column represents the level of the duey item.
     * It is an integer with a default value of 0.
     */
    val level = integer("level").default(0)

    /**
     * This column represents the strength of the duey item.
     * It is an integer with a default value of 0.
     */
    val str = integer("str").default(0)

    /**
     * This column represents the dexterity of the duey item.
     * It is an integer with a default value of 0.
     */
    val dex = integer("dex").default(0)

    /**
     * This column represents the intelligence of the duey item.
     * It is an integer with a default value of 0.
     */
    val int = integer("int").default(0)

    /**
     * This column represents the luck of the duey item.
     * It is an integer with a default value of 0.
     */
    val luk = integer("luk").default(0)

    /**
     * This column represents the health points of the duey item.
     * It is an integer with a default value of 0.
     */
    val hp = integer("hp").default(0)

    /**
     * This column represents the magic points of the duey item.
     * It is an integer with a default value of 0.
     */
    val mp = integer("mp").default(0)

    /**
     * This column represents the weapon attack of the duey item.
     * It is an integer with a default value of 0.
     */
    val watk = integer("watk").default(0)

    /**
     * This column represents the magic attack of the duey item.
     * It is an integer with a default value of 0.
     */
    val matk = integer("matk").default(0)

    /**
     * This column represents the weapon defense of the duey item.
     * It is an integer with a default value of 0.
     */
    val wdef = integer("wdef").default(0)

    /**
     * This column represents the magic defense of the duey item.
     * It is an integer with a default value of 0.
     */
    val mdef = integer("mdef").default(0)

    /**
     * This column represents the accuracy of the duey item.
     * It is an integer with a default value of 0.
     */
    val acc = integer("acc").default(0)

    /**
     * This column represents the avoidance of the duey item.
     * It is an integer with a default value of 0.
     */
    val avoid = integer("avoid").default(0)

    /**
     * This column represents the hands of the duey item.
     * It is an integer with a default value of 0.
     */
    val hands = integer("hands").default(0)

    /**
     * This column represents the speed of the duey item.
     * It is an integer with a default value of 0.
     */
    val speed = integer("speed").default(0)

    /**
     * This column represents the jump of the duey item.
     * It is an integer with a default value of 0.
     */
    val jump = integer("jump").default(0)

    /**
     * This column represents the owner of the duey item.
     * It is a varchar column with a maximum length of 13 characters.
     */
    val owner = varchar("owner", 13)

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}