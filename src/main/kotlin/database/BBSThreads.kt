package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object BBSThreads : Table() {
    val threadId = integer("threadId").autoIncrement()
    val posterCid = integer("posterCid")
    val name = varchar("name", 26).default("")
    val timestamp = timestamp("timestamp")
    val icon = short("icon")
    val replyCount = short("replyCount").default(0)
    val startPost = text("startPost")
    val guildId = integer("guildId")
    val localThreadId = integer("localThreadId")
    override val primaryKey = PrimaryKey(threadId)
}