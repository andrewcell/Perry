package webapi.controller

import database.Accounts
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun validateAdmin(principal: JWTPrincipal): Boolean {
    val gm = (principal.payload.getClaim("gm").asInt() ?: -1)
    val isAdmin = gm > 1
    val name = principal.payload.getClaim("name").asString()
    val id = principal.payload.getClaim("id").asInt()
    var validated = false
    transaction {
        val account = Accounts.slice(
            Accounts.id, Accounts.name, Accounts.gm
        ).select { (Accounts.name eq name) and (Accounts.id eq id) }
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