package database

import org.jetbrains.exposed.sql.Table

object DueyPackages : Table() {
    val packageId = integer("packageId").autoIncrement()
    val receiverId = integer("receiverId")
    val senderName = varchar("senderName", 13)
    val mesos = integer("mesos").default(0)
    val timestamp = varchar("timestamp", 10)
    val checked = bool("checked").default(true)
    val type = byte("type")
    override val primaryKey = PrimaryKey(packageId)
}