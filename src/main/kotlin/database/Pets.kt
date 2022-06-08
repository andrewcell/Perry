package database

import org.jetbrains.exposed.sql.Table

object Pets : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 13)
    val level = integer("level")
    val closeness = integer("closeness")
    val fullness = integer("fullness")
    val summoned = bool("summoned")
    override val primaryKey = PrimaryKey(id)
}