package webapi.controller

import database.Accounts
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.server.handlers.login.AutoRegister
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import webapi.tools.ApiResponse
import webapi.tools.ResponseMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@kotlinx.serialization.Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val birthday: String,
    val female: Boolean,
    val socialNumber: Int
)

fun Route.account() {
    route("/account") {
        post("/register") {
            val data = call.receive<RegisterRequest>()
            val requestIP = call.request.origin.remoteHost
            // Check it is already registered with same IP.
            var failed = false
            var reason: ResponseMessage = ResponseMessage.SUCCESS
            transaction {
                val row = Accounts.select { (Accounts.sessionIp eq requestIP) or (Accounts.name eq data.email) }.toList()
                val ip = row.count { it[Accounts.sessionIp] == requestIP }
                val name = row.count { it[Accounts.name] == data.email }
                if (ip + name >= 1) {
                    failed = true
                    reason = if (ip >= 1) ResponseMessage.ALREADY_REGISTERED_IP
                        else if (name >= 1) ResponseMessage.ALREADY_REGISTERED_EMAIL
                        else ResponseMessage.BAD_REQUEST
                    return@transaction
                }
            }
            if (!failed) {
                val birthday = try {
                    LocalDate.parse(data.birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                } catch (e: DateTimeParseException) {
                    null
                }
                try {
                    AutoRegister.registerAccount(requestIP, data.email, data.password, data.female, birthday)
                } catch (e: Exception) {
                    failed = true
                    reason = ResponseMessage.INTERNAL_ERROR
                }
            }
            if (failed) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse(false, reason))
            } else {
                call.respond(ApiResponse(true, reason))
            }
        }
    }
}