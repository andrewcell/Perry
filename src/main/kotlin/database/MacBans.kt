package database

import org.jetbrains.exposed.sql.Table

object MacBans : Table() {
    val id = integer("id").autoIncrement()
    val mac = varchar("mac", 30)
    override val primaryKey = PrimaryKey(id)
}