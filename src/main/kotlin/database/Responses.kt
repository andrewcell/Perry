package database

import org.jetbrains.exposed.sql.Table

object Responses : Table() {
    val id = integer("id").autoIncrement()
    val chat = text("chat")
    val response = text("response")
    override val primaryKey = PrimaryKey(id)
}