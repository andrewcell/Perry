package webapi.tools

import net.server.Server

/**
 * Retrieve Online players count for Web API.
 *
 * @author Andrew M. Bray
 * @since 1.0.0
 */

class OnlinePlayers {
    companion object {
        /**
         * Get all online players across the worlds.
         *
         * @return Online players count
         */
        fun getAllOnlinePlayers(): Int {
            var count = 0
            Server.worlds.forEach {
                count += it.players.getCount()
            }
            return count
        }

        /**
         * Get online players in selected world.
         *
         * @param world WorldId
         * @return Online players count of that world
         */
        fun getOnlinePlayersByWorld(world: Short): Int {
            return Server.getWorld(world.toInt()).players.getCount()
        }
    }
}