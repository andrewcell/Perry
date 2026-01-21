package gm.server.handler

import client.Client
import database.Accounts
import gm.GMPacketCreator
import gm.GMPacketHandler
import gm.server.GMServer
import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey
import mu.KLogging
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tools.data.input.SeekableLittleEndianAccessor
import java.sql.SQLException

class LoginHandler : GMPacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, session: ChannelHandlerContext?) {
        if (GMServer.KEYWORD != slea.readGameASCIIString()) {
            session?.write(GMPacketCreator.sendLoginResponse(-1, ""))
            return
        }
        val login = slea.readGameASCIIString()
        if (GMServer.contains(login)) {
            session?.write(GMPacketCreator.sendLoginResponse(0, ""))
            return
        }
        val password = slea.readGameASCIIString()
        try {
            transaction {
                val row = Accounts.select(Accounts.password, Accounts.id).where {
                    (Accounts.name eq login) and (Accounts.gm greaterEq 2)
                }
                if (!row.empty()) {
                    val first = row.first()
                    val pw = first[Accounts.password]
                    val salt = first[Accounts.salt] ?: ""
                    if (Client.checkHash(pw, password, salt)) {
                        session?.let { session ->
                            session.channel().attr(AttributeKey.valueOf<String>("NAME")).set(login)
                            GMServer.addOutGame(login, session.channel())
                            session.write(GMPacketCreator.sendLoginResponse(3, login))
                            GMServer.broadcastOutGame(GMPacketCreator.chat("$login has logged in."), login)
                            GMServer.broadcastOutGame(GMPacketCreator.addUser(login), login)
                            session.write(GMPacketCreator.sendUserList(GMServer.getUserList(login)))
                        }
                        return@transaction
                    }
                } else {
                    session?.write(GMPacketCreator.sendLoginResponse(1, ""))
                }
            }
            session?.write(GMPacketCreator.sendLoginResponse(1, ""))
        } catch (e: SQLException) {
            logger.error(e) { "Failed to handle login packet. LoginId: $login" }
        }
    }

    companion object : KLogging()
}