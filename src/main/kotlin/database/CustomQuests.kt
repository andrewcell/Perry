package database

import org.jetbrains.exposed.v1.core.Table

object CustomQuests : Table() {
    val id = integer("id").autoIncrement()
    val characterId = integer("characterId")
    val questId = integer("questId")
    val status = integer("status")
    override val primaryKey = PrimaryKey(id)
}