package webapi.controller

import constants.ServerConstants
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import net.server.Server
import webapi.tools.OnlinePlayers

@Serializable
data class ServerStatusWeb(
    val online: Boolean,
    val worlds: Int,
    val targetClientVersion: String,
    val playersOnline: Int
)
fun Route.index() {
    route("/") {
        authenticate("auth") {
            get("/status") {
                val status = ServerStatusWeb(
                    Server.online,
                    Server.worlds.size,
                    "1.2.${ServerConstants.gameVersion}",
                    OnlinePlayers.getAllOnlinePlayers()
                )
                call.respond(status)
            }
        }
    }
}