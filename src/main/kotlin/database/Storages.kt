package database

import org.jetbrains.exposed.v1.core.Table

/**
 * This object represents the Storages table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object Storages : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the account ID associated with the storage.
     * It is an integer and references the id column in the Accounts table.
     */
    val accountId = integer("accountId").references(Accounts.id)

    /**
     * This column represents the world associated with the storage.
     * It is an integer.
     */
    val world = integer("world")

    /**
     * This column represents the number of slots in the storage.
     * It is an integer with a default value of 0.
     */
    val slots = integer("slots").default(0)

    /**
     * This column represents the amount of meso in the storage.
     * It is an integer with a default value of 0.
     */
    val meso = integer("meso").default(0)

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}