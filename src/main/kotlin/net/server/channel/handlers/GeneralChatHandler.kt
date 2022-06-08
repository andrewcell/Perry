package net.server.channel.handlers

//import client.command.Commands
import client.Client
import client.command.Commands
import mu.KLogging
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket

class GeneralChatHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chatSize = slea.readShort()
        val s = slea.readASCIIString(chatSize.toInt())
        val chr = c.player
        val heading = s[0]
        try {
            chr?.let { player ->
                when (heading) {
                    '/', '!', '@' -> {
                        val sp = s.split(" ").toTypedArray()
                        sp[0] = sp[0].lowercase().substring(1)
                        if (chr.isGM()) {
                            Commands.executeGMCommand(c, sp, heading)
                        } else {
                            /*if (!Commands.executePlayerCommand(c, sp, heading)) {
                            if (chr.isGM() || chr.haveItem(ServerConstants.itemId, 1)) {
                                if (!Commands.executeGMCommand(c, sp, heading)) {
                                    Commands.executeAdminCommand(c, sp, heading)
                                }
                            }
                        } else {*/
                            if (!player.hidden) {
                                player.map.broadcastMessage(
                                    InteractPacket.getChatText(
                                        player.id,
                                        s,
                                        player.isGM(),
                                        chatSize.toInt()
                                    )
                                )
                            } else {
                                player.map.broadcastGMMessage(
                                    InteractPacket.getChatText(
                                        player.id,
                                        s,
                                        player.isGM(),
                                        chatSize.toInt()
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                        if (!player.hidden) {
                            player.map.broadcastMessage(
                                InteractPacket.getChatText(
                                    player.id,
                                    s,
                                    player.isGM(),
                                    chatSize.toInt()
                                )
                            )
                        } else {
                            player.map.broadcastGMMessage(
                                InteractPacket.getChatText(
                                    player.id,
                                    s,
                                    player.isGM(),
                                    chatSize.toInt()
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error caused handle general chat." }
        }
    }

    companion object : KLogging()
}