package database

import org.jetbrains.exposed.v1.core.Table

object DropData : Table() {
    val id = integer("id").autoIncrement()
    val dropperId = integer("dropperId")
    val itemId = integer("itemId")
    val minimumQuantity = integer("minimumQuantity")
    val maximumQuantity = integer("maximumQuantity")
    val questId = integer("questId")
    val chance = integer("chance")
    override val primaryKey = PrimaryKey(id)
}