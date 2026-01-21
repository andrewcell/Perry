package database

import org.jetbrains.exposed.v1.core.Table

object EventStats : Table() {
    val characterId = integer("characterId")
    val name = varchar("name", 11).default("0")
    val instance = varchar("instance", 45).default("")
    val channel = integer("channel")
    val info = integer("info")
    override val primaryKey = PrimaryKey(characterId)
}