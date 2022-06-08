package net.server.handlers.login

import client.Client
import database.Accounts
import mu.KLogging
import org.bouncycastle.util.encoders.Hex
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tools.PasswordHash
import java.sql.SQLException

class AutoRegister {
    companion object : KLogging() {
        private const val ENABLE_IP_COUNT = 10

        fun checkAccount(c: Client, name: String, password: String): Int {
            try {
                var code = 5
                transaction {
                    val account = Accounts.select {
                        Accounts.name eq name
                    }.toList()
                    if (account.isNotEmpty()) return@transaction
                    val sessionIp = Accounts.slice(Accounts.sessionIp).select {
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

        fun registerAccount(c: Client, name: String, password: String, female: Boolean) {
            try {
                val salt = PasswordHash.generateSalt()
                transaction {
                    with (Accounts) {
                        this.insert {
                            it[this.name] = name
                            it[this.password] = PasswordHash.generate(password, salt)
                            it[this.salt] = Hex.toHexString(salt)
                            it[this.sessionIp] = c.getSessionIPAddress()
                            it[this.gender] = if (female) 1 else 0
                        }
                    }
                }
            } catch (e: SQLException) {
                logger.error(e) { "Failed to register account to database." }
            }
        }
    }
}