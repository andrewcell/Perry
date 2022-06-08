package net.server.channel.handlers

import client.Client
import database.Characters
import mu.KLogging
import net.AbstractPacketHandler
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket
import java.sql.SQLException

class WhisperHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.player?.let { player ->
            when (slea.readByte().toInt()) {
                6 -> { // whisper
                    val recipient = slea.readGameASCIIString()
                    val text = slea.readGameASCIIString()
                    val targetPlayer = c.getChannelServer().players.getCharacterByName(recipient)
                    if (targetPlayer != null) {
                        targetPlayer.client.announce(InteractPacket.getWhisper(targetPlayer.name, c.channel, text))
                        c.announce(InteractPacket.getWhisperReply(recipient, 1))
                    } else {
                        val world = c.getWorldServer()
                        val reply  = if (world.isConnected(recipient)) {
                            world.whisper(player.name, recipient, c.channel, text)
                            1
                        } else 0
                        c.announce(InteractPacket.getWhisperReply(recipient, reply.toByte()))
                    }
                }
                5 -> { // find
                    transaction {
                        val recipient = slea.readGameASCIIString()
                        val victim = c.getChannelServer().players.getCharacterByName(recipient)
                        if (victim != null && player.gmLevel >= victim.gmLevel) {
                            if (victim.cashShop?.opened == true) {
                                c.announce(InteractPacket.getWhisperFindReply(victim.name, -1, 2))
                            } else {
                                c.announce(InteractPacket.getWhisperFindReply(victim.name, victim.map.mapId, 1))
                            }
                        } else {
                            try {
                                val row = Characters.select { Characters.name eq recipient }
                                if (!row.empty()) {
                                    if (row.first()[Characters.gm] > player.gmLevel) {
                                        c.announce(InteractPacket.getWhisperReply(recipient, 0))
                                        return@transaction
                                    }
                                }
                                val channel = c.getWorldServer().findChannelIdByCharacterName(recipient) - 1
                                if (channel > -1) {
                                    c.announce(InteractPacket.getWhisperFindReply(recipient, channel, 3))
                                } else {
                                    c.announce(InteractPacket.getWhisperReply(recipient, 0))
                                }
                            } catch (e: SQLException) {
                                logger.error(e) { "Failed to find character from database." }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object : KLogging()
}