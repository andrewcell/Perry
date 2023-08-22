package webapi.controller

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import tools.ServerJSON
import webapi.tools.ApiResponse
import webapi.tools.ResponseMessage
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger { }

fun Route.adminServer() {
    authenticate("auth") {
        route("/admin/server") {
            post("/shutdown") {
                val principal = call.principal<JWTPrincipal>() ?: return@post
                val name = principal.payload.getClaim("name").asString()
                val id = principal.payload.getClaim("id").asInt()
                if (validateAdmin(principal)) {
                    call.respond(ApiResponse(true, ResponseMessage.SUCCESS))
                    logger.warn { "Warning. Server is going to be shut down. triggered by Admin $name, Id: $id" }
                    exitProcess(0)
                    //Runtime.getRuntime().addShutdownHook(Thread(Server.shutdown(false)))
                }
            }
            post("/reload") { // Reload server configuration
                val principal = call.principal<JWTPrincipal>() ?: return@post
                val name = principal.payload.getClaim("name").asString()
                val id = principal.payload.getClaim("id").asInt()
                if (validateAdmin(principal)) {
                    ServerJSON.reload()
                    call.respond(ApiResponse(true, ResponseMessage.SUCCESS))
                    logger.info { "Warning. Server has been reloaded. triggered by Admin $name, Id: $id" }
                }
            }
        }
    }
}