package gm.server.handler

import gm.GMPacketCreator
import gm.GMPacketHandler
import io.netty.channel.ChannelHandlerContext
import net.server.Server
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket

class CommandHandler : GMPacketHandler {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, session: ChannelHandlerContext?) {
        val command = slea.readByte()
        when (command.toInt()) {
            0 -> { // notice
                Server.worlds.forEach {
                    it.broadcastPacket(InteractPacket.serverNotice(0, slea.readGameASCIIString()))
                }
            }
            1 -> { // server message
                Server.worlds.forEach {
                    it.setServerMessage(slea.readGameASCIIString())
                }
            }
            2 -> {
                val worldId = slea.readByte()
                if (worldId >= Server.worlds.size) {
                    session?.write(GMPacketCreator.commandResponse(2))
                    return
                }
                val world = Server.getWorld(worldId.toInt())
                /*switch (slea.readByte()) {
                    case 0:
                        world.setExpRate(slea.readByte());
                        break;
                    case 1:
                        world.setDropRate(slea.readByte());
                        break;
                    case 2:
                        world.setMesoRate(slea.readByte());
                        break;
                }
                for (Character chr : world.getPlayerStorage().getAllCharacters()) {
                    chr.setRates();
                }*/
            }
            3 -> {
                val user = slea.readGameASCIIString()
                Server.worlds.forEach {
                    if (it.isConnected(user)) {
                        it.players.getCharacterByName(user)?.client?.disconnect(shutdown = false, cashShop = false)
                        session?.write(GMPacketCreator.commandResponse(1))
                        return
                    }
                }
            }
            4 -> {
                val user = slea.readGameASCIIString()
                Server.worlds.forEach {
                    if (it.isConnected(user)) {
                        val chr = it.players.getCharacterByName(user)
                        chr?.ban(slea.readGameASCIIString())
                        session?.write(GMPacketCreator.commandResponse(1))
                        return
                    }
                }
                session?.write(GMPacketCreator.commandResponse(0))
            }
            5 -> {
                val user = slea.readGameASCIIString()
                Server.worlds.forEach {
                    if (it.isConnected(user)) {
                        val chr = it.players.getCharacterByName(user)
                        val job = "${chr?.job?.name} (${chr?.job?.id})"
                        chr?.let { c ->
                            session?.write(GMPacketCreator.playerStats(user, job, c.level.toByte(), c.exp.get(),
                                c.maxHp.toShort(),
                                c.maxMp.toShort(),
                                c.str.toShort(),
                                c.dex.toShort(),
                                c.int.toShort(),
                                c.luk.toShort(), c.meso.get()))
                        }
                    }
                }
                session?.write(GMPacketCreator.commandResponse(0))
            }
            7 -> Server.shutdown(false).run()
            8 -> Server.shutdown(true).run()
        }
    }
}