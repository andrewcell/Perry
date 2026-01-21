package database

import org.jetbrains.exposed.v1.core.Table

object NXCodes : Table() {
    val code = varchar("code", 15)
    val valid = integer("valid")
    val user = varchar("user", 13)
    val type = integer("type").default(0)
    val item = integer("item").default(10000)
    override val primaryKey = PrimaryKey(code)
}