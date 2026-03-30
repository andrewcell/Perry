package webapi.controller

import client.Client
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import database.Accounts
import database.Characters
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import mu.KotlinLogging
import net.server.Server
import net.server.handlers.login.AutoRegister
import org.bouncycastle.util.encoders.Hex
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import tools.CoroutineManager
import tools.PasswordHash
import webapi.tools.ApiResponse
import webapi.tools.JWTVariables
import webapi.tools.ResponseMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

/**
 * Represents a request to register a new user account.
 *
 * Contains the necessary information for account creation, including authentication credentials,
 * personal details (birthday and gender), and official identification data.
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val birthday: String,
    val female: Boolean,
    val socialNumber: Int
)

/**
 * Represents a request to authenticate a user.
 *
 * Contains the necessary credentials (email and password) for login,
 * along with an optional request token used in specific authentication flows.
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val requestToken: String? = null
)

/**
 * Represents detailed information about an account.
 *
 * Contains core account metadata and status-related fields,
 * including login activity, ban state, registration details, and associated identifiers.
 *
 * All fields except [name] are optional, allowing partial representation of account data.
 *
 * @property name The unique display name of the account holder.
 * @property nxCredit Optional balance of NX credit associated with the account.
 * @property mPoint Optional balance of M Point currency.
 * @property lastLogin Optional Unix timestamp (milliseconds) of the last login attempt; null if never logged in.
 * @property createdAt Optional Unix timestamp (milliseconds) when the account was created; null if unknown.
 * @property banned Optional indication of whether the account is currently banned.
 * @property banReason Optional human-readable reason for the current ban status, if applicable.
 * @property female Optional gender indicator: true for female, false for male, null if unspecified or unknown.
 * @property socialNumber Optional integer identifier used for social/external reference purposes.
 * @property registeredIP Optional IP address string recorded at account registration time.
 */
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

/**
 * Represents a request to change an authenticated user's password.
 *
 * The request must include the user's current credentials and the new desired password,
 * with explicit confirmation of the new password. All fields are required for validation.
 */
@Serializable
data class ChangePasswordRequest(
    val name: String,
    val oldPassword: String,
    val newPassword: String,
    val newPasswordCheck: String,
)

private val logger = KotlinLogging.logger {  }

/**
 * Registers a Ktor [Route] for account-related operations under the "/account" path.
 *
 * This route group includes:
 * - `/register`: POST endpoint to register a new account, validating uniqueness of email and IP,
 *   parsing birthday, and invoking [AutoRegister.registerAccount].
 * - `/login`: POST endpoint to authenticate users by comparing hashed password with stored salt,
 *   returning a JWT token upon success.
 * - Sub-routes under authentication:
 *   - `/info`: GET endpoint to retrieve detailed account information for the authenticated user.
 *   - `/info`: PUT endpoint to update the authenticated user's gender and social number.
 *   - `/delete`: DELETE endpoint to disconnect all active sessions associated with the user’s account ID
 *     before account deletion (implementation truncated in snippet).
 *
 * All responses use [ApiResponse] structured with status messages from [ResponseMessage],
 * appropriate HTTP status codes, and optional payload data when needed.
 */
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
                    Accounts.select(Accounts.sessionIp, Accounts.name).where { (Accounts.sessionIp eq requestIP) or (Accounts.name eq data.email) }.toList()
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
                    val account = Accounts.select(Accounts.salt).where { Accounts.name eq user.email }
                    if (account.empty()) return@transaction
                    val row = Accounts.select(Accounts.name, Accounts.id, Accounts.gm).where {
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
                        if (acc[Accounts.gm] >= 1)
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
                        .select(
                            Accounts.name, Accounts.nxCredit, Accounts.mPoint,
                            Accounts.lastLogin, Accounts.createdAt,
                            Accounts.banned, Accounts.banReason, Accounts.gender,
                            Accounts.socialNumber, Accounts.sessionIp)
                        .where((Accounts.id eq id) eq (Accounts.name eq name))
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
                var statusCode = HttpStatusCode.OK
                var message = ResponseMessage.SUCCESS
                val principal = call.principal<JWTPrincipal>() ?: return@delete
                val id = principal.payload.getClaim("id").asInt()
                try {
                    transaction {
                        Server.worlds.forEach { world ->
                            world.players.storage.values.find { it.accountId == id }?.client?.disconnect(
                                shutdown = false,
                                cashShop = false
                            )
                        }
                        Characters.select(Characters.accountId eq id).forEach {
                            CoroutineManager.schedule({
                                Client.deleteCharacter(it[Characters.id], id)
                            }, 0)
                        }
                        Accounts.deleteWhere { Accounts.id eq id }
                    }
                } catch (e: Exception) {
                    logger.error(e) { e.message }
                    statusCode = HttpStatusCode.InternalServerError
                    message = ResponseMessage.INTERNAL_ERROR
                } finally {
                    call.respond(statusCode, ApiResponse(message == ResponseMessage.SUCCESS, message))
                }
            }
            put("/password") {
                val body = call.receive<ChangePasswordRequest>()
                var statusCode = HttpStatusCode.OK
                var message = ResponseMessage.SUCCESS
                val principal = call.principal<JWTPrincipal>() ?: return@put
                val id = principal.payload.getClaim("id").asInt()
                val name = principal.payload.getClaim("name").asString()
                if (body.newPassword != body.newPasswordCheck) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse(false, ResponseMessage.PASSWORD_CHECK_MISMATCH))
                    return@put
                }
                try {
                    transaction {
                        val row = Accounts.select(Accounts.password, Accounts.salt).where((Accounts.id eq id) and (Accounts.name eq name)).firstOrNull()
                        if (row == null) {
                            statusCode = HttpStatusCode.InternalServerError
                            message = ResponseMessage.INTERNAL_ERROR
                        } else {
                            val oldMatch = row[Accounts.password] == PasswordHash.generate(row[Accounts.password], Hex.decode(row[Accounts.salt]))
                            if (!oldMatch) {
                                statusCode = HttpStatusCode.BadRequest
                                message = ResponseMessage.INCORRECT_OLD_PASSWORD
                                return@transaction
                            }
                            Accounts.update({ Accounts.id eq id }) {
                                val newSalt = PasswordHash.generateSalt()
                                it[password] = PasswordHash.generate(body.newPassword, newSalt)
                                it[salt] = Hex.toHexString(newSalt)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { e.message }
                    statusCode = HttpStatusCode.InternalServerError
                    message = ResponseMessage.INTERNAL_ERROR
                } finally {
                    call.respond(statusCode, ApiResponse(message == ResponseMessage.SUCCESS, message))
                }
            }
        }
    }
}