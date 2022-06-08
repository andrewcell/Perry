package client.inventory

import database.Pets
import mu.KLogging
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import server.ItemInformationProvider
import server.movement.AbsoluteLifeMovement
import server.movement.LifeMovement
import server.movement.LifeMovementFragment
import java.awt.Point
import java.sql.SQLException

class Pet(id: Int, position: Byte, val uniqueId: Int) : Item(id, position, 1) {
    var name = ""
    var closeness = 0
    var level: Byte = 1
    var fullness = 100
    var fh = -1
    var stance = 0
    var pos: Point? = null
    var summoned = false

    fun canConsume(itemId: Int): Boolean {
        return ItemInformationProvider.petsCanConsume(itemId).any { it == itemId }
    }

    fun gainCloseness(x: Int) {
        closeness += x
    }

    fun saveToDatabase() {
        try {
            transaction {
                Pets.update({ Pets.id eq uniqueId }) {
                    it[name] = this@Pet.name
                    it[level] = this@Pet.level.toInt()
                    it[closeness] = this@Pet.closeness
                    it[fullness] = this@Pet.fullness
                    it[summoned] = this@Pet.summoned
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save pet data to database. PetId: $petId" }
        }
    }

    fun updatePosition(movement: List<LifeMovementFragment>?) {
        movement?.forEach {
            if (it is LifeMovement) {
                if (it is AbsoluteLifeMovement) {
                    pos = it.position
                }
                stance = it.newState.toInt()
            }

        }
    }

    companion object : KLogging() {
        fun createPet(itemId: Int): Int {
            try {
                var id = -1
                transaction {
                    val pet = Pets.insert {
                        it[name] = ItemInformationProvider.getName(itemId) ?: ""
                        it[level] = 1
                        it[closeness] = 0
                        it[fullness] = 100
                        it[summoned] = false
                    }.resultedValues ?: return@transaction
                    if (pet.first().hasValue(Pets.id)) {
                        id = pet.first()[Pets.id]
                    } else return@transaction
                }
                return id
            } catch (e: SQLException) {
                logger.error(e) { "Failed to save created pet data in database. ItemId: $itemId" }
            }
            return -1
        }

        fun createPet(itemId: Int, level: Byte, closeness: Int, fullness: Int): Int {
            try {
                var ret = -1
                transaction {
                    val pet = Pets.insert {
                        it[name] = ItemInformationProvider.getName(itemId) ?: ""
                        it[Pets.level] = level.toInt()
                        it[Pets.closeness] = closeness
                        it[Pets.fullness] = fullness
                    }.resultedValues ?: return@transaction
                    if (pet.first().hasValue(Pets.id)) {
                        ret = pet.first()[Pets.id]
                    } else return@transaction
                }
                return ret
            } catch (e: SQLException) {
                logger.error(e) { "Failed to save created pet data in database. ItemId: $itemId" }
            }
            return -1
        }

        fun loadFromDatabase(itemId: Int, position: Byte, petId: Int): Pet? {
            try {
                val ret = Pet(itemId, position, petId)
                transaction {
                    val petRs = Pets.select { Pets.id eq petId }
                    if (petRs.empty()) return@transaction
                    val pet = petRs.first()
                    ret.name = pet[Pets.name]
                    ret.closeness = pet[Pets.closeness].coerceAtMost(30000)
                    ret.level = pet[Pets.level].toByte().coerceAtMost(30)
                    ret.fullness = pet[Pets.fullness].coerceAtMost(100)
                    ret.summoned = pet[Pets.summoned]
                }
                return ret
            } catch (e: SQLException) {
                logger.error(e) { "Failed to load pet from database. petId: $petId" }
            }
            return null
        }
    }
}