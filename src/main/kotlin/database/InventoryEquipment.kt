package database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object InventoryEquipment : Table() {
    val id = integer("id").autoIncrement()
    val inventoryItemId = integer("inventoryItemId").references(InventoryItems.id, ReferenceOption.CASCADE)
    val upgradableSlots = integer("upgradableSLots").default(0)
    val level = integer("level").default(0)
    val str = integer("str").default(0)
    val dex = integer("dex").default(0)
    val int = integer("int").default(0)
    val luk = integer("luk").default(0)
    val hp = integer("hp").default(0)
    val mp = integer("mp").default(0)
    val watk = integer("watk").default(0)
    val matk = integer("matk").default(0)
    val wdef = integer("wdef").default(0)
    val mdef = integer("mdef").default(0)
    val acc = integer("acc").default(0)
    val avoid = integer("avoid").default(0)
    val hands = integer("hands").default(0)
    val speed = integer("speed").default(0)
    val jump = integer("jump").default(0)
    val locked = integer("locked")
    val vicious = integer("vicious").default(0)
    val itemLevel = integer("itemLevel").default(1)
    val itemExp = float("itemExp").default(0f)
    val ringId = integer("ringId").default(-1)
    override val primaryKey = PrimaryKey(id)
}