package database

import org.jetbrains.exposed.v1.core.Table

object PlayerNpcsEquip : Table() {
    val id = integer("id").autoIncrement()
    val npcId = integer("npcId").default(0)
    val equipId = integer("equipId")
    val type = integer("type").default(0)
    val equipPos = integer("equipPos")
    override val primaryKey = PrimaryKey(id)
}