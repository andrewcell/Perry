package webapi

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import mu.KLoggable
import org.slf4j.event.Level
import tools.ServerJSON

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
                })
            }
            install(Routing) {
                index()
            }
        }.start(wait = false)
    }
}