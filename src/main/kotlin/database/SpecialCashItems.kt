package database

import org.jetbrains.exposed.v1.core.Table

/**
 * This object represents the SpecialCashItems table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object SpecialCashItems : Table() {
    /**
     * This is the primary key column of the table.
     * It is an integer.
     */
    val id = integer("id")

    /**
     * This column represents the serial number associated with the special cash item.
     * It is an integer.
     */
    val sn = integer("sn")

    /**
     * This column represents the modifier associated with the special cash item.
     * It is an integer.
     */
    val modifier = integer("modifier")

    /**
     * This column represents the info associated with the special cash item.
     * It is an integer.
     */
    val info = integer("info")

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}