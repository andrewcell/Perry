/**
 * This package contains the database related classes and objects.
 */
package database

import org.jetbrains.exposed.sql.Table

/**
 * This object represents the Wishlists table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object Wishlists : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the character ID associated with the wishlist.
     * It is an integer.
     */
    val charId = integer("charId")

    /**
     * This column represents the serial number associated with the wishlist.
     * It is an integer.
     */
    val sn = integer("sn")

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}