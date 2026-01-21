package database

import org.jetbrains.exposed.v1.core.Table

object PlayerNpcs : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 13)
    val hair = integer("hair")
    val face = integer("face")
    val skin = integer("skin")
    val x = integer("x")
    val cy = integer("cy").default(0)
    val map = integer("map")
    val gender = integer("gender").default(0)
    val dir = integer("dir").default(0)
    val scriptId = integer("scriptId").default(0)
    val foothold = integer("foothold").default(0)
    val rx0 = integer("rx0").default(0)
    val rx1 = integer("rx1").default(0)
    override val primaryKey = PrimaryKey(id)
}