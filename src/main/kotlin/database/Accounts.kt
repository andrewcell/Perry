package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.time.LocalDate

object Accounts : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50).default("")
    val password = varchar("password", 128).default("")
    val salt = varchar("salt", 128).nullable()
    val loggedIn = integer("loggedIn").default(0)
    val lastLogin = timestamp("lastLogin").nullable()
    val createdAt = timestamp("createdAt").clientDefault { Instant.now() }
    val birthday = date("birthday").default(LocalDate.of(1,1,1))
    val banned = bool("banned").default(false)
    val banReason = text("banReason").nullable()
    val gm = integer("gm").default(0)
    val macs = text("macs").nullable()
    val nxCredit = integer("nxCredit").nullable()
    val nxPrepaid = integer("nxPrepaid").nullable()
    val mPoint = integer("mPoint").nullable()
    val charactorSlots = integer("charSlots").default(5)
    val gender = integer("gender").default(0)
    val tempBan = timestamp("tempBan").default(Instant.parse("1970-01-01T00:00:00Z"))
    val gReason = integer("gReason").default(0)
    val tos = integer("tos").default(0)
    val socialNumber = integer("ssn").default(1_234_567)
    val sessionIp = varchar("sessionIP", 64)
    override val primaryKey = PrimaryKey(id, name = "PK_Account_Id")
}