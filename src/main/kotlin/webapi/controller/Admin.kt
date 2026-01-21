package webapi.controller

import database.Accounts
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun validateAdmin(principal: JWTPrincipal): Boolean {
    val gm = (principal.payload.getClaim("gm").asInt() ?: -1)
    val isAdmin = gm > 1
    val name = principal.payload.getClaim("name").asString() ?: ""
    val id = principal.payload.getClaim("id").asInt() ?: -1
    if (!isAdmin || name == "" || id == 0) return false
    var validated = false
    transaction {
        val account = Accounts.select(
            Accounts.id, Accounts.name, Accounts.gm
        ).where { (Accounts.name eq name) and (Accounts.id eq id) }
        if (account.empty()) return@transaction
        val accountRow = account.first()
        val dbName = accountRow[Accounts.name]
        val dbId = accountRow[Accounts.id]
        val dbGm = accountRow[Accounts.gm]
        if (dbName != name || dbId != id || dbGm != gm) return@transaction
        validated = true
    }
    return validated
    // Validate is it real admin account.
}

fun Route.admin() {
    authenticate("auth") {
        route("/admin") {
        }
    }
}