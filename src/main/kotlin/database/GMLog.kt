package database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object GMLog : Table() {
    val id = integer("id").autoIncrement()
    val cid = integer("cid")
    val command = text("command")
    val timestamp = timestamp("timestamp").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}