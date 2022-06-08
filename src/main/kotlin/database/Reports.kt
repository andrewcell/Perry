package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Reports : Table() {
    val id = integer("id").autoIncrement()
    val reportTime = timestamp("reportTime").clientDefault { Instant.now() }
    val victimId = integer("victimId")
    val reason = byte("reason")
    val chatLog = text("chatlog")
    val status = text("status")
    val description = varchar("description", 45)
    override val primaryKey = PrimaryKey(id)
}