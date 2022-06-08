package database

import org.jetbrains.exposed.sql.Table

object HtSquads : Table() {
    val id = integer("id").autoIncrement()
    val channel = integer("channel")
    val leaderId = integer("leaderId")
    val status = integer("status")
    val members = integer("members")
    override val primaryKey = PrimaryKey(id)
}