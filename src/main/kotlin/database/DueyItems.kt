package database

import org.jetbrains.exposed.sql.Table

object DueyItems : Table() {
    val id = integer("id").autoIncrement()
    val packageId = integer("packageId").references(DueyPackages.packageId)
    val itemId = integer("itemId")
    val quantity = integer("quantity")
    val upgradeSlots = integer("upgradeSlots").default(0)
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
    val owner = varchar("owner", 13)
    override val primaryKey = PrimaryKey(id)
}