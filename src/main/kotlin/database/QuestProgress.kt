package database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object QuestProgress : Table() {
    val id = integer("id").autoIncrement()
    val questStatusId = integer("questStatusId").references(QuestStatuses.id, ReferenceOption.CASCADE)
    val progressId = integer("progressId").default(0)
    val progress = varchar("progress", 15).default("")
    override val primaryKey = PrimaryKey(id)
}