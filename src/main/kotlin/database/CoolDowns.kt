package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object CoolDowns : Table() {
    val id = integer("id").autoIncrement()
    val charId = integer("charId")
    val skillId = integer("skillId")
    val length = long("length")
    val startTime = timestamp("startTime").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}