package database

import org.jetbrains.exposed.sql.Table

object Skills : Table() {
    val id = integer("id").autoIncrement()
    val characterId = integer("characterId").references(Characters.id)
    val skillId = integer("skillId").default(0)
    val skillLevel = integer("skillLevel").default(0)
    val masterLevel = integer("masterLevel").default(0)
    val expiration = long("expiration").default(-1)
    override val primaryKey = PrimaryKey(id)
}