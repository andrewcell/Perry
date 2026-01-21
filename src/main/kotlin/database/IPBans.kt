package database

import org.jetbrains.exposed.v1.core.Table

object IPBans : Table() {
    val id = integer("id").autoIncrement()
    val ip = varchar("ip", 40)
    override val primaryKey = PrimaryKey(id)
}