package webapi.controller

import database.Accounts
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import mu.KotlinLogging
import net.server.Server
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import webapi.tools.ApiResponse
import webapi.tools.ResponseMessage

private val logger = KotlinLogging.logger { }
fun Route.adminServer() {
    authenticate("auth") {
        route("/admin/server") {
            post("/shutdown") {
                val principal = call.principal<JWTPrincipal>() ?: return@post
                val gm = (principal.payload.getClaim("gm").asInt() ?: -1)
                val isAdmin = gm > 1
                val name = principal.payload.getClaim("name").asString()
                val id = principal.payload.getClaim("id").asInt()
                if (!isAdmin || name == null || id == null) return@post
                var validated = false
                transaction { // Validate username and id is not malformed
                    val account = Accounts.slice(Accounts.id, Accounts.name, Accounts.gm).select { Accounts.name eq name }
                    if (account.empty()) return@transaction
                    val accountEntry = account.first()
                    val dbName = accountEntry[Accounts.name]
                    val dbId = accountEntry[Accounts.id]
                    val dbGm = accountEntry[Accounts.gm]
                    if (dbName != name || dbId != id || dbGm != gm) return@transaction
                    validated = true
                }
                if (validated) {
                    logger.error { "Warning. Server is going to be shut down. triggered by Admin $name, Id: $id, GM Level: $gm" }
                    Runtime.getRuntime().addShutdownHook(Thread(Server.shutdown(false)))
                }
            }
        }
    }
}