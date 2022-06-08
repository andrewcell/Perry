package gm.server.handler

import gm.GMPacketCreator
import gm.GMPacketHandler
import io.netty.channel.ChannelHandlerContext
import net.server.Server
import tools.data.input.SeekableLittleEndianAccessor

class PlayerListHandler : GMPacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, session: ChannelHandlerContext?) {
        val playerList = mutableListOf<String>()
        Server.getAllChannels().forEach {
            val list = it.players.getAllCharacters()
            synchronized(list) {
                list.forEach { chr ->
                    if (!chr.isGM()) playerList.add(chr.name)
                }
            }
        }
        session?.write(GMPacketCreator.sendPlayerList(playerList))
    }
}