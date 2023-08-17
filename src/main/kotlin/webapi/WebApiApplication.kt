package webapi

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import mu.KLoggable
import org.slf4j.event.Level
import tools.ServerJSON
import webapi.controller.*
import webapi.tools.ApiResponse
import webapi.tools.JWTVariables
import webapi.tools.ResponseMessage

object WebApiApplication : KLoggable {
    override val logger = logger()

    fun main() {
        embeddedServer(Netty, port = ServerJSON.settings.webApi.port) {
            install(CallLogging) {
                level = Level.INFO
                filter { call -> call.request.path().startsWith("/") }
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    logger.error(cause) { "Exception caught in WebAPI. Request IP: ${call.request.origin.remoteHost}" }
                    call.respondText("What is happening in here?", status = HttpStatusCode.BadRequest)
                }
            }
            install(Authentication) {
                jwt("auth") {
                    realm = JWTVariables.myRealm
                    verifier(JWT
                        .require(Algorithm.HMAC256(JWTVariables.secret))
                        .withAudience(JWTVariables.audience)
                        .withIssuer(JWTVariables.issuer)
                        .build())
                    validate { credential ->
                        if (credential.payload.getClaim("username").asString() != "")
                            JWTPrincipal(credential.payload)
                        else null
                    }
                    challenge { _, _ ->
                        call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, ResponseMessage.UNAUTHORIZED))
                    }
                }
            }
            install(Routing) {
                index()
                account()
                adminServer()
                adminSearch()
                admin()
            }
        }.start(wait = false)
    }
}