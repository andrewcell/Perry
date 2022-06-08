package database

import org.jetbrains.exposed.sql.Table

object SavedLocations : Table() {
    enum class LocationType {
        FREE_MARKET, FLORINA, WORLDTOUR, INTRO, MIRROR, EVENT
    }
    val id = integer("id").autoIncrement()
    val characterId = integer("characterId").references(Characters.id)
    val locationType = text("locationType")
    val map = integer("map")
    val portal = integer("portal")
    override val primaryKey = PrimaryKey(id)
}