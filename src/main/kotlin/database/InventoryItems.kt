package database

import org.jetbrains.exposed.v1.core.Table

object InventoryItems : Table() {
    val id = integer("id").autoIncrement()
    val type = byte("type")
    val characterId = integer("characterId")
    val accountId = integer("accountId")
    val itemId = integer("itemId").default(0)
    val inventoryType = integer("inventoryType").default(0)
    val position = integer("position").default(0)
    val quantity = integer("quantity").default(0)
    val owner = text("owner")
    val petId = integer("petId").default(-1)
    val flag = integer("flag")
    val expiration = long("expiration").default(-1)
    val giftFrom = varchar("giftFrom", 26)
    override val primaryKey = PrimaryKey(id)
}