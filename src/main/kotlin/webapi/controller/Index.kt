package webapi.controller

import constants.ServerConstants
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import net.server.Server
import webapi.tools.OnlinePlayers

/**
 * Represents the current status of the server as exposed through a web API endpoint.
 *
 * Contains key metrics and version information useful for monitoring server health and compatibility.
 */
@Serializable
data class ServerStatusWeb(
    val online: Boolean,
    val worlds: Int,
    val targetClientVersion: String,
    val playersOnline: Int
)

/**
 * Registers the index route for server status retrieval.
 *
 * This route exposes an endpoint at `/` that handles `GET /status` requests,
 * which requires JWT authentication via the "auth" interceptor. Upon successful
 * authentication, it constructs and responds with a [ServerStatusWeb] object
 * containing current server state information including online status, world count,
 * client version, and total number of online players across all worlds.
 */
fun Route.index() {
    route("/") {
        authenticate("auth") {
            get("/status") {
                val status = ServerStatusWeb(
                    Server.online,
                    Server.worlds.size,
                    "1.2.${ServerConstants.GAME_VERSION}",
                    OnlinePlayers.getAllOnlinePlayers()
                )
                call.respond(status)
            }
        }
    }
}