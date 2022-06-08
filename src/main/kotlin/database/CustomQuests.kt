package database

import org.jetbrains.exposed.sql.Table

object CustomQuests : Table() {
    val id = integer("id").autoIncrement()
    val characterId = integer("characterId")
    val questId = integer("questId")
    val status = integer("status")
    override val primaryKey = PrimaryKey(id)
}