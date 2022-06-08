package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object BBSReplies : Table() {
    val replyId = integer("replyId").autoIncrement()
    val threadId = integer("threadId")
    val posterCid = integer("posterCid")
    val timestamp = timestamp("timestamp")
    val content = varchar("content", 26).default("")
    override val primaryKey = PrimaryKey(replyId, name = "PK_BBS_REPLIES")
}