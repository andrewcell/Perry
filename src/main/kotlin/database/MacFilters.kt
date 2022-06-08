package database

import org.jetbrains.exposed.sql.Table

object MacFilters : Table() {
    val id = integer("id").autoIncrement()
    val mac = varchar("mac", 30)
    override val primaryKey = PrimaryKey(id)
}