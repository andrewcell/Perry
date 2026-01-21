package database

import org.jetbrains.exposed.v1.core.Table

object KeyValues : Table() {
    val cid = integer("cid").references(Characters.id)
    val key = varchar("key", 50).default("")
    val value = varchar("value", 100).default("")
    override val primaryKey = PrimaryKey(cid)
}