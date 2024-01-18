package database

import org.jetbrains.exposed.sql.Table

/**
 * This object represents the SavedLocations table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object SavedLocations : Table() {
    /**
     * This enum represents the different types of locations that can be saved.
     * It includes FREE_MARKET, FLORINA, WORLDTOUR, INTRO, MIRROR, and EVENT.
     */
    enum class LocationType {
        FREE_MARKET, FLORINA, WORLDTOUR, INTRO, MIRROR, EVENT
    }

    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the character ID associated with the saved location.
     * It is an integer and references the id column in the Characters table.
     */
    val characterId = integer("characterId").references(Characters.id)

    /**
     * This column represents the type of the saved location.
     * It is a text column and stores the name of the LocationType enum.
     */
    val locationType = text("locationType")

    /**
     * This column represents the map ID of the saved location.
     * It is an integer.
     */
    val map = integer("map")

    /**
     * This column represents the portal ID of the saved location.
     * It is an integer.
     */
    val portal = integer("portal")

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}