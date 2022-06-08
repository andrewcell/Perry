package database

import org.jetbrains.exposed.sql.Table

object Guilds : Table() {
    val guildId = integer("guildId").autoIncrement()
    val leader = integer("leader")
    val GP = integer("gp")
    val logo = integer("logo")
    val logoColor = short("logoColor")
    val logoBG = integer("logoBG")
    val logoBGColor = short("logoBGColor")
    val name = varchar("name", 45)
    val rank1Title = varchar("rank1Title", 45).default("마스터")
    val rank2Title = varchar("rank2Title", 45).default("부마스터")
    val rank3Title = varchar("rank3Title", 45).default("길드원")
    val rank4Title = varchar("rank4Title", 45).default("길드원")
    val rank5Title = varchar("rank5Title", 45).default("길드원")
    val notice = varchar("notice", 101)
    val capacity = integer("capacity").default(10)
    val signature = integer("signature").default(0)
    override val primaryKey = PrimaryKey(guildId)
}