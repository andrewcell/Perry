/**
 * This package contains the database related classes and objects.
 */
package database

import org.jetbrains.exposed.sql.Table

/**
 * This object represents the TrockLocations table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object TrockLocations : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the character ID associated with the TrockLocation.
     * It is an integer and references the id column in the Characters table.
     */
    val characterId = integer("characterId").references(Characters.id)

    /**
     * This column represents the map ID associated with the TrockLocation.
     * It is an integer.
     */
    val mapId = integer("mapId")

    /**
     * This column represents the VIP status associated with the TrockLocation.
     * It is an integer.
     */
    val vip = integer("vip")

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}