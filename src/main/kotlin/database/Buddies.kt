package database

import org.jetbrains.exposed.v1.core.Table

object Buddies : Table() {
    val id = integer("id").autoIncrement()
    val characterId = integer("characterId").references(Characters.id)
    val buddyId = integer("buddyId")
    val pending = byte("pending").default(0)
    val group = varchar("group", 17).default("0")
    override val primaryKey = PrimaryKey(id)
}