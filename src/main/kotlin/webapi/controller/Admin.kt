package webapi.controller

import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.admin() {
    authenticate("auth") {
        route("/admin") {
        }
    }
}