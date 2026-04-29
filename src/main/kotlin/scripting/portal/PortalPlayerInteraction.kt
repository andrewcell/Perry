package scripting.portal

import client.Client
import database.Characters
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import scripting.AbstractPlayerInteraction
import server.Portal
import tools.packet.GameplayPacket
import java.sql.SQLException

/**
 * Provides portal-specific interactions for players during scripted portal events.
 *
 * This class extends [AbstractPlayerInteraction] to add functionality specific to
 * portal scripts, including portal blocking/unblocking and level requirement checks.
 *
 * @property c The client associated with this interaction
 * @property portal The portal being interacted with
 */
class PortalPlayerInteraction(c: Client, val portal: Portal) : AbstractPlayerInteraction(c) {
    private val logger = KotlinLogging.logger {  }

    /**
     * Checks if the player's account has any character at level 30 or above.
     *
     * This is commonly used to verify eligibility for certain portals or content
     * that requires at least one experienced character on the account.
     *
     * @return `true` if the account has at least one character at level 30 or higher,
     *         `false` otherwise or if a database error occurs
     */
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

    /**
     * Blocks the current portal for this player.
     *
     * When blocked, the player will not be able to use this portal until it is unblocked.
     * Uses the portal's script name as the identifier.
     */
    fun blockPortal() = portal.scriptName?.let { c.player?.blockPortal(it) }

    /**
     * Unblocks the current portal for this player.
     *
     * Allows the player to use this portal again after it was previously blocked.
     * Uses the portal's script name as the identifier.
     */
    fun unblockPortal() = portal.scriptName?.let { c.player?.unblockPortal(it) }

    /**
     * Plays the standard portal enter sound effect for the client.
     */
    fun playPortalSound() = c.announce(GameplayPacket.playPortalSound())
}