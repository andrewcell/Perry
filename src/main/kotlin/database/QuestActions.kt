package database

import org.jetbrains.exposed.v1.core.Table

object QuestActions : Table() {
    val id = integer("id").autoIncrement()
    val questId = integer("questId").default(0)
    val status = integer("status").default(0)
    val data = blob("data")
    override val primaryKey = PrimaryKey(id)
}