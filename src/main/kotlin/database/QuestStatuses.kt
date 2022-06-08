package database

import org.jetbrains.exposed.sql.Table

object QuestStatuses : Table() {
    val id = integer("id").autoIncrement()
    val characterId = integer("characterId").references(Characters.id)
    val quest = integer("quest")
    val status = integer("status")
    val time = integer("time")
    val forfeited = integer("forfeited")
    override val primaryKey = PrimaryKey(id)
}