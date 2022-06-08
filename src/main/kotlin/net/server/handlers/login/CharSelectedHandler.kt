package net.server.handlers.login

import client.Client
import mu.KLoggable
import net.AbstractPacketHandler
import net.server.Server
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.LoginPacket
import java.net.InetAddress
import java.net.UnknownHostException

class CharSelectedHandler : AbstractPacketHandler(), KLoggable {
    override val logger = logger()
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