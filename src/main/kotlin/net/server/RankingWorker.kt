package net.server

import client.GameJob
import database.Accounts
import database.Characters
import kotlinx.coroutines.Runnable
import mu.KLoggable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.sql.SQLException

class RankingWorker : Runnable, KLoggable {
    override val logger = logger()
    private var lastUpdate = System.currentTimeMillis()

    @Throws(SQLException::class)
    private fun updateRanking(job: GameJob? = null) {
        transaction {
            var row = (Characters leftJoin Accounts).select { Characters.gm eq 0 }

            if (job != null) row = row.andWhere { (Characters.job div 100) eq (job.id / 100) } // If job is null, it is for character level ranking
            row = row.orderBy(Characters.level, SortOrder.DESC)
                .orderBy(Characters.exp, SortOrder.DESC)
                .orderBy(Characters.fame, SortOrder.DESC)
                .orderBy(Characters.meso, SortOrder.DESC)
            var rank = 0
            row.forEach { chr ->
                var rankMove = 0
                rank++
                if ((chr[Accounts.lastLogin]?.toEpochMilli() ?: 0) < lastUpdate || chr[Accounts.loggedIn] > 0) {
                    rankMove = if (job != null) chr[Characters.jobRankMove] else chr[Characters.rankMove]
                }
                rankMove += if (job != null) chr[Characters.jobRank] else chr[Characters.rank] - rank
                Characters.update({ Characters.id eq chr[Characters.id] }) {
                    if (job != null) {
                        it[jobRank] = rank
                        it[jobRankMove] = rankMove
                    } else {
                        it[Characters.rank] = rank
                        it[Characters.rankMove] = rankMove
                    }
                }
            }
            logger.info {
                if (job == null)
                    "Ranking worker updated Overall ranking."
                else
                    "Ranking worker updated Job ranking for ${job.name}."
            }
        }
    }

    override fun run() {
        try {
            updateRanking()
            for (j in 1..5) {
                updateRanking(GameJob.getById(100 * j))
            }
            lastUpdate = System.currentTimeMillis()
        } catch (e: Exception) {
            logger.error(e) { "Error caused when update ranking."}
        }
    }
}