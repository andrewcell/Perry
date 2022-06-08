package database

import org.jetbrains.exposed.sql.Table

object TrockLocations : Table() {
    val id = integer("id").autoIncrement()
    val characterId = integer("characterId").references(Characters.id)
    val mapId = integer("mapId")
    val vip = integer("vip")
    override val primaryKey = PrimaryKey(id)
}