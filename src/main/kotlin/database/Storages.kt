package database

import org.jetbrains.exposed.sql.Table

object Storages : Table() {
    val id = integer("id").autoIncrement()
    val accountId = integer("accountId").references(Accounts.id)
    val world = integer("world")
    val slots = integer("slots").default(0)
    val meso = integer("meso").default(0)
    override val primaryKey = PrimaryKey(id)
}