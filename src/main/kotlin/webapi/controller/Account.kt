package webapi.controller

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.beust.klaxon.JsonObject
import database.Accounts
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import mu.KotlinLogging
import net.server.handlers.login.AutoRegister
import org.bouncycastle.util.encoders.Hex
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.timestampLiteral
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import tools.PasswordHash
import webapi.tools.ApiResponse
import webapi.tools.JWTVariables
import webapi.tools.ResponseMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

@kotlinx.serialization.Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val birthday: String,
    val female: Boolean,
    val socialNumber: Int
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val requestToken: String? = null
)

@Serializable
data class AccountInfo(
    val name: String,
    val nxCredit: Int?,
    val mPoint: Int?,
    val lastLogin: Long?,
    val createdAt: Long?,
    val banned: Boolean?,
    val banReason: String?,
    val female: Boolean?,
    val socialNumber: Int?,
    val registeredIP: String?
)

private val logger = KotlinLogging.logger {  }
fun Route.account() {
    route("/account") {
        post("/register") {
            val data = call.receive<RegisterRequest>()
            val requestIP = call.request.origin.remoteHost
            // Check it is already registered with same IP.
            var failed = false
            var reason: ResponseMessage = ResponseMessage.SUCCESS
            transaction {
                val row =
                    Accounts.select { (Accounts.sessionIp eq requestIP) or (Accounts.name eq data.email) }.toList()
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
        post("login") {
            val user = call.receive<LoginRequest>()
            var login: String? = null
            var id: Int? = null
            var gm: Int? = null
            try {
                transaction {
                    val account = Accounts.slice(Accounts.salt).select { Accounts.name eq user.email }
                    if (account.empty()) return@transaction
                    val row = Accounts.select {
                        (Accounts.name eq user.email) and (Accounts.password eq PasswordHash.generate(
                            user.password,
                            Hex.decode(account.first()[Accounts.salt] ?: "")
                        ))
                    }
                    if (row.empty()) {
                        return@transaction
                    } else {
                        val acc = row.first()
                        login = acc[Accounts.name]
                        id = acc[Accounts.id]
                        gm = acc[Accounts.gm]
                    }
                }
                if (login == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiResponse(false, ResponseMessage.INCORRECT_EMAIL_PASSWORD)
                    )
                } else {
                    val token = JWT.create()
                        .withAudience(JWTVariables.audience)
                        .withIssuer(JWTVariables.issuer)
                        .withClaim("name", login)
                        .withClaim("id", id)
                        .withClaim("gm", gm)
                        .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                        .sign(Algorithm.HMAC256(JWTVariables.secret))
                    call.respond(ApiResponse(true, ResponseMessage.SUCCESS, Json.encodeToJsonElement(mapOf("token" to token))))
                }
            } catch (e: Exception) {
                logger.error(e) { e.message }
                call.respond(HttpStatusCode.InternalServerError, ApiResponse.internalError)
            }
        }
        authenticate("auth") { // Require token
            get("/info") {
                val principal = call.principal<JWTPrincipal>() ?: return@get
                val id = principal.payload.getClaim("id").asInt()
                val name = principal.payload.getClaim("name").asString()
                var message = ResponseMessage.SUCCESS
                var statusCode = HttpStatusCode.OK
                var data: AccountInfo? = null
                transaction {
                    val row = Accounts
                        .slice(
                            Accounts.name, Accounts.nxCredit, Accounts.mPoint,
                            Accounts.lastLogin, Accounts.createdAt,
                            Accounts.banned, Accounts.banReason, Accounts.gender,
                            Accounts.socialNumber, Accounts.sessionIp)
                        .select((Accounts.id eq id) eq (Accounts.name eq name))
                        .limit(1)
                        .firstOrNull()
                    if (row == null) {
                        message = ResponseMessage.INTERNAL_ERROR
                        statusCode = HttpStatusCode.InternalServerError
                    } else {
                        data = AccountInfo(
                            name = row[Accounts.name],
                            nxCredit = row[Accounts.nxCredit] ?: 0,
                            mPoint = row[Accounts.mPoint] ?: 0,
                            lastLogin = row[Accounts.lastLogin]?.toEpochMilli() ?: 0,
                            createdAt = row[Accounts.createdAt].toEpochMilli(),
                            banned = row[Accounts.banned],
                            banReason = row[Accounts.banReason] ?: "",
                            female = row[Accounts.gender] == 1,
                            socialNumber = row[Accounts.socialNumber],
                            registeredIP = row[Accounts.sessionIp]
                        )
                    }
                }
                call.respond(statusCode, ApiResponse(message == ResponseMessage.SUCCESS, message, Json.encodeToJsonElement(data)))
            }
            put("/info") {
                var statusCode = HttpStatusCode.OK
                var message = ResponseMessage.SUCCESS
                val body = call.receive<AccountInfo>()
                val principal = call.principal<JWTPrincipal>() ?: return@put
                val id = principal.payload.getClaim("id").asInt()
                val name = principal.payload.getClaim("name").asString()
                if (body.name != name) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse(false, ResponseMessage.BAD_REQUEST))
                    return@put
                }
                try {
                    transaction {
                        Accounts.update({ (Accounts.name eq name) and (Accounts.id eq id) }) { s ->
                            body.female?.let { s[gender] = if (it) 1 else 0 }
                            body.socialNumber?.let { s[socialNumber] = it }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { e.message }
                    statusCode = HttpStatusCode.InternalServerError
                    message = ResponseMessage.SUCCESS
                } finally {
                    call.respond(statusCode, message)
                }
            }
            delete("/delete") {

            }
        }
    }
}