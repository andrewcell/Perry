package database

import org.jetbrains.exposed.v1.core.Table
/**
 * This object represents the Skills table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object Skills : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer.
     */
    val id = integer("id").autoIncrement()

    /**
     * This column represents the character ID associated with the skill.
     * It is an integer and references the id column in the Characters table.
     */
    val characterId = integer("characterId").references(Characters.id)

    /**
     * This column represents the ID of the skill.
     * It is an integer with a default value of 0.
     */
    val skillId = integer("skillId").default(0)

    /**
     * This column represents the level of the skill.
     * It is an integer with a default value of 0.
     */
    val skillLevel = integer("skillLevel").default(0)

    /**
     * This column represents the master level of the skill.
     * It is an integer with a default value of 0.
     */
    val masterLevel = integer("masterLevel").default(0)

    /**
     * This column represents the expiration of the skill.
     * It is a long with a default value of -1.
     */
    val expiration = long("expiration").default(-1)

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}