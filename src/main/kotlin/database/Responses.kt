/**
 * This package contains the database related classes and objects.
 */
package database

import org.jetbrains.exposed.sql.Table

/**
 * This object represents the Responses table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object Responses : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the chat associated with the response.
     * It is a text column.
     */
    val chat = text("chat")

    /**
     * This column represents the response associated with the chat.
     * It is a text column.
     */
    val response = text("response")

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}