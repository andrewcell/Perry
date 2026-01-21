package database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object Notes : Table() {
    val id = integer("id").autoIncrement()
    val to = varchar("to", 13).default("")
    val from = varchar("from",13).default("")
    val message = text("message")
    val timestamp = timestamp("timestamp").clientDefault { Instant.now() }
    val fame = integer("fame").default(0)
    val deleted = integer("notes").default(0)
    override val primaryKey = PrimaryKey(id)
}