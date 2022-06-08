package database

import org.jetbrains.exposed.sql.Table

object MonsterBooks : Table() {
    val charId = integer("charId").references(Characters.id)
    val cardId = integer("cardId")
    val level = integer("level").default(1)
    override val primaryKey = PrimaryKey(charId, cardId)
}