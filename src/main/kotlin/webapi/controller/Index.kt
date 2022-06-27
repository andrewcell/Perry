package webapi

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.index() {
    route("/") {
        get {
            call.respondText { "It works!" }
        }

        get("/json") {
            call.respond(mapOf(Pair("Hello", "World!")))
        }
    }
}