package net.server.handlers.login

import client.Client
import database.Accounts
import mu.KLogging
import org.bouncycastle.util.encoders.Hex
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tools.PasswordHash
import java.sql.SQLException
import java.time.LocalDate

class AutoRegister {
    companion object : KLogging() {
        private const val ENABLE_IP_COUNT = 10

        fun checkAccount(c: Client, name: String, password: String): Int {
            try {
                var code = 5
                transaction {
                    val account = Accounts.select(Accounts.sessionIp).where {
                        Accounts.name eq name
                    }.toList()
                    if (account.isNotEmpty()) return@transaction
                    val sessionIp = Accounts.select(Accounts.sessionIp).where {
                        Accounts.sessionIp eq c.getSessionIPAddress()
                    }.map { it[Accounts.sessionIp] }
                    code = if (sessionIp.size >= ENABLE_IP_COUNT) 6
                        else if (sessionIp.isNotEmpty() || sessionIp.size < ENABLE_IP_COUNT) 0
                        else 5
                }
                return code
            } catch (e: SQLException) {
                logger.error(e) { "Failed to check account from database." }
            }
            return 5
        }

        fun registerAccount(sessionIp: String, name: String, password: String, female: Boolean, birthday: LocalDate? = null) {
            try {
                val salt = PasswordHash.generateSalt()
                transaction {
                    with (Accounts) {
                        this.insert {
                            it[this.name] = name
                            it[this.password] = PasswordHash.generate(password, salt)
                            it[this.salt] = Hex.toHexString(salt)
                            it[this.sessionIp] = sessionIp
                            it[this.gender] = if (female) 1 else 0
                            if (birthday != null) {
                                it[this.birthday] = birthday
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to register account to database." }
            }
        }
    }
}