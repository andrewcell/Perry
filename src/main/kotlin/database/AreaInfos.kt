package database

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object AreaInfos : IntIdTable() {
    val charId = integer("charid")
    val area = integer("area")
    val info = varchar("info", 120)
}