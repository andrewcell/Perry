package database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object FameLog : Table() {
    val fameLogId = integer("fameLogId").autoIncrement()
    val characterId = integer("characterId").references(Characters.id)
    val characterIdTo = integer("characterIdTo")
    val timestamp = timestamp("when").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(fameLogId)
}