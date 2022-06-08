package database

import org.jetbrains.exposed.sql.Table

object Wishlists : Table() {
    val id = integer("id").autoIncrement()
    val charId = integer("charId")
    val sn = integer("sn")
    override val primaryKey = PrimaryKey(id)
}