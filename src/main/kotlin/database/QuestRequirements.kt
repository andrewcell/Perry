package database

import org.jetbrains.exposed.v1.core.Table

object QuestRequirements : Table() {
    val id = integer("id").autoIncrement()
    val questId = integer("questId")
    val status = integer("status")
    val data = blob("data")
    override val primaryKey = PrimaryKey(id)
}