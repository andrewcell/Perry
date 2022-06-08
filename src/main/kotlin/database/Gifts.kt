package database

import org.jetbrains.exposed.sql.Table

object Gifts : Table() {
    val id = integer("id").autoIncrement()
    val to = integer("to").references(Characters.id)
    val from = varchar("from", 13)
    val message = text("message")
    val sn = integer("sn")
    val ringId = integer("ringId")
    override val primaryKey = PrimaryKey(id)
}