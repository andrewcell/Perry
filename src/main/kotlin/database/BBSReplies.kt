package database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

object BBSReplies : Table() {
    val replyId = integer("replyId").autoIncrement()
    val threadId = integer("threadId")
    val posterCid = integer("posterCid")
    val timestamp = timestamp("timestamp")
    val content = varchar("content", 26).default("")
    override val primaryKey = PrimaryKey(replyId, name = "PK_BBS_REPLIES")
}