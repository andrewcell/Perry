package net.server.handlers.login

import client.Client
import mu.KLoggable
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class CharListRequestHandler : AbstractPacketHandler(), KLoggable {
    override val logger = logger()

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        slea.readByte()
        val world = slea.readByte().toInt()
        c.world = world
        c.channel = slea.readByte() + 1
        logger.info { "Client(${c.getSessionIPAddress()}), User(${c.accountName}) entered World $world, Channel ${c.channel}." }
        c.sendCharList(world)
    }

}