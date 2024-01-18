/**
 * This package contains the database related classes and objects.
 */
package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * This object represents the Reports table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object Reports : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the time when the report was made.
     * It is a timestamp with a default value of the current time.
     */
    val reportTime = timestamp("reportTime").clientDefault { Instant.now() }

    /**
     * This column represents the ID of the victim associated with the report.
     * It is an integer.
     */
    val victimId = integer("victimId")

    /**
     * This column represents the reason for the report.
     * It is a byte.
     */
    val reason = byte("reason")

    /**
     * This column represents the chat log associated with the report.
     * It is a text column.
     */
    val chatLog = text("chatlog")

    /**
     * This column represents the status of the report.
     * It is a text column.
     */
    val status = text("status")

    /**
     * This column represents the description of the report.
     * It is a varchar column with a maximum length of 45 characters.
     */
    val description = varchar("description", 45)

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}