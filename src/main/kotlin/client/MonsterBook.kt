package client

import database.MonsterBooks
import mu.KLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import tools.packet.MonsterBookPacket
import java.sql.SQLException
import kotlin.math.max
import kotlin.math.sqrt

class MonsterBook {
    private var specialCard = 0
    private var normalCard = 0
    private var bookLevel = 1
    val cards = mutableMapOf<Int, Int>()

    fun addCard(c: Client, newCardId: Int) {
        c.player?.let { player ->
            player.map.broadcastMessage(player, MonsterBookPacket.showForeignCardEffect(player.id), false)
        }
        for ((cardId, level) in cards) {
            if (level > 4) {
                c.announce(MonsterBookPacket.addCard(true, cardId, level))
            } else {
                cards[cardId] = level + 1
                c.announce(MonsterBookPacket.addCard(false, cardId, level))
                c.announce(MonsterBookPacket.showGainCard())
                calculateLevel()
            }
            return
        }
        cards[newCardId] = 1
        c.announce(MonsterBookPacket.addCard(false, newCardId, 1))
        c.announce(MonsterBookPacket.showGainCard())
        calculateLevel()
        c.player?.saveToDatabase()
    }

    private fun calculateLevel() {
        bookLevel = max(1.0, sqrt(((normalCard + specialCard) / 5).toDouble())).toInt()
    }

    fun getTotalCards() = specialCard + normalCard

    fun loadCards(charId: Int) {
        try {
            transaction {
                MonsterBooks
                    .slice(MonsterBooks.cardId, MonsterBooks.level)
                    .select {
                        MonsterBooks.charId eq charId }
                    .orderBy(MonsterBooks.cardId, order = SortOrder.ASC)
                    .forEach {
                        val cardId = it[MonsterBooks.cardId]
                        if (cardId / 1000 >= 2388) specialCard++
                        else normalCard++
                        cards[cardId] = it[MonsterBooks.level]
                    }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to load monster cards from database." }
        }
        calculateLevel()
    }

    fun saveCards(charId: Int) {
        if (cards.isEmpty()) return
        try {
            transaction {
                MonsterBooks.deleteWhere { MonsterBooks.charId eq charId }
                cards.forEach { (cardId, level) ->
                    MonsterBooks.insert {
                        it[MonsterBooks.charId] = charId
                        it[MonsterBooks.cardId] = cardId
                        it[MonsterBooks.level] = level
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save monster cards to database." }
        }
    }

    companion object : KLogging()
}