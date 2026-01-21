package scripting.portal

import client.Client
import database.Characters
import mu.KLoggable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import scripting.AbstractPlayerInteraction
import server.Portal
import tools.packet.GameplayPacket
import java.sql.SQLException

class PortalPlayerInteraction(c: Client, val portal: Portal) : AbstractPlayerInteraction(c), KLoggable {
    override val logger = logger()

    fun hasLevel30Character(): Boolean {
        try {
            var result = false
            transaction {
                getPlayer()?.let {
                    Characters.select(Characters.level).where { Characters.accountId eq it.accountId }
                        .forEach {
                            if (it[Characters.level] >= 30) result = true
                        }
                }
            }
            return result
        } catch (e: SQLException) {
            logger.error(e) { "Failed check has 30 level character. PortalId: ${portal.id}" }
        }
        return false
    }

    fun blockPortal() = portal.scriptName?.let { c.player?.blockPortal(it) }

    fun unblockPortal() = portal.scriptName?.let { c.player?.unblockPortal(it) }

    fun playPortalSound() = c.announce(GameplayPacket.playPortalSound())
}