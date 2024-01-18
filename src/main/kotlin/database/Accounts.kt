/**
 * This package contains the database related classes and objects.
 */
package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.time.LocalDate

/**
 * This object represents the Accounts table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object Accounts : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the name associated with the account.
     * It is a varchar column with a maximum length of 50 characters and a default value of an empty string.
     * It is also unique.
     */
    val name = varchar("name", 50).default("").uniqueIndex("name")

    /**
     * This column represents the password associated with the account.
     * It is a varchar column with a maximum length of 128 characters and a default value of an empty string.
     */
    val password = varchar("password", 128).default("")

    /**
     * This column represents the salt associated with the account.
     * It is a nullable varchar column with a maximum length of 128 characters.
     */
    val salt = varchar("salt", 128).nullable()

    /**
     * This column represents the logged in status of the account.
     * It is an integer with a default value of 0.
     */
    val loggedIn = integer("loggedIn").default(0)

    /**
     * This column represents the last login time of the account.
     * It is a nullable timestamp.
     */
    val lastLogin = timestamp("lastLogin").nullable()

    /**
     * This column represents the creation time of the account.
     * It is a timestamp with a default value of the current time.
     */
    val createdAt = timestamp("createdAt").clientDefault { Instant.now() }

    /**
     * This column represents the birthday associated with the account.
     * It is a date with a default value of 0001-01-01.
     */
    val birthday = date("birthday").default(LocalDate.of(1,1,1))

    /**
     * This column represents the banned status of the account.
     * It is a boolean with a default value of false.
     */
    val banned = bool("banned").default(false)

    /**
     * This column represents the ban reason associated with the account.
     * It is a nullable text column.
     */
    val banReason = text("banReason").nullable()

    /**
     * This column represents the gm status of the account.
     * It is an integer with a default value of 0.
     */
    val gm = integer("gm").default(0)

    /**
     * This column represents the macs associated with the account.
     * It is a nullable text column.
     */
    val macs = text("macs").nullable()

    /**
     * This column represents the nxCredit associated with the account.
     * It is a nullable integer.
     */
    val nxCredit = integer("nxCredit").nullable()

    /**
     * This column represents the nxPrepaid associated with the account.
     * It is a nullable integer.
     */
    val nxPrepaid = integer("nxPrepaid").nullable()

    /**
     * This column represents the mPoint associated with the account.
     * It is a nullable integer.
     */
    val mPoint = integer("mPoint").nullable()

    /**
     * This column represents the character slots associated with the account.
     * It is an integer with a default value of 5.
     */
    val charactorSlots = integer("charSlots").default(5)

    /**
     * This column represents the gender associated with the account.
     * It is an integer with a default value of 0.
     */
    val gender = integer("gender").default(0)

    /**
     * This column represents the temporary ban time of the account.
     * It is a timestamp with a default value of 1970-01-01T00:00:00Z.
     */
    val tempBan = timestamp("tempBan").default(Instant.parse("1970-01-01T00:00:00Z"))

    /**
     * This column represents the gReason associated with the account.
     * It is an integer with a default value of 0.
     */
    val gReason = integer("gReason").default(0)

    /**
     * This column represents the tos associated with the account.
     * It is an integer with a default value of 0.
     */
    val tos = integer("tos").default(0)

    /**
     * This column represents the social number associated with the account.
     * It is an integer with a default value of 1234567.
     */
    val socialNumber = integer("ssn").default(1_234_567)

    /**
     * This column represents the session IP associated with the account.
     * It is a varchar column with a maximum length of 64 characters.
     */
    val sessionIp = varchar("sessionIP", 64)

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id, name = "PK_Account_Id")
}