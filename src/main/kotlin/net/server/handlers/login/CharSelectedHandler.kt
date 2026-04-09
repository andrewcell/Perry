package net.server.handlers.login

import client.Client
import io.github.oshai.kotlinlogging.KotlinLogging
import net.AbstractPacketHandler
import net.server.Server
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.LoginPacket
import java.net.InetAddress
import java.net.UnknownHostException

class CharSelectedHandler : AbstractPacketHandler() {
    private val logger = KotlinLogging.logger {  }

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val charId = slea.readInt()
        c.idleTask?.cancel()
        c.updateLoginState(Client.LOGIN_SERVER_TRANSITION)
        try {
            val socket = Server.getIp(c.world, c.channel)?.split(":") ?: throw UnknownHostException()
            c.announce(LoginPacket.getServerIP(InetAddress.getByName(socket[0]), socket[1].toInt(), charId))
        } catch (e: UnknownHostException) {
            logger.warn(e) { "Failed to resolve server IP. please check settings file or report problem." }
        }
    }
}