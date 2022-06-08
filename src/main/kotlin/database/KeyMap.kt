package database

import org.jetbrains.exposed.sql.Table

object KeyMap : Table() {
    val id = integer("id").autoIncrement()
    val characterId = integer("characterId").references(Characters.id)
    val key = integer("key").default(0)
    val type = integer("type").default(0)
    val action = integer("action").default(0)
    override val primaryKey = PrimaryKey(id)
}