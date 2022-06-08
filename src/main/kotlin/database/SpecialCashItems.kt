package database

import org.jetbrains.exposed.sql.Table

object SpecialCashItems : Table() {
    val id = integer("id")
    val sn = integer("sn")
    val modifier = integer("modifier")
    val info = integer("info")
    override val primaryKey = PrimaryKey(id)
}