package database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object IPLogs : Table(){
    val id = integer("id").autoIncrement()
    val accountId = integer("accountId").references(Accounts.id)
    val ip = varchar("ip", 30).default("")
    val loginTime = timestamp("loginTime").clientDefault { Instant.now() }
    override val primaryKey = PrimaryKey(id)
}