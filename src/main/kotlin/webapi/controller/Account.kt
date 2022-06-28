package webapi.controller

import io.ktor.server.routing.*

fun Route.account() {
    route("/account") {
        post("register") {

        }
    }
}