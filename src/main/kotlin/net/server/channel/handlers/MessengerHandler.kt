package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import net.server.world.MessengerCharacter
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket

class MessengerHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val mode = slea.readByte()
        val player = c.player ?: return
        val world = c.getWorldServer()
        var messenger = player.messenger
        when (mode.toInt()) {
            0x00 -> {
                if (messenger == null) {
                    val messengerId = slea.readInt()
                    if (messengerId == 0) {
                        val messengerPlayer = MessengerCharacter(player, 0)
                        messenger = world.createMessenger(messengerPlayer)
                        player.messenger = messenger
                        player.messengerPosition = 0
                    } else {
                        messenger = world.getMessenger(messengerId)
                        val position = messenger?.getLowestPosition(false) ?: 1
                        val messengerPlayer = MessengerCharacter(player, position)
                        if (messenger?.members?.size == 0) {
                            c.player?.announce(InteractPacket.joinMessenger(-1))
                            return
                        }
                        if (messenger?.members?.size!! < 3) {
                            player.messenger = messenger
                            player.messengerPosition = position
                            world.joinMessenger(messenger.id, messengerPlayer, player.name, messengerPlayer.channel)
                        }
                    }
                }
            }
            0x02 -> {
                if (messenger != null) {
                    val messengerPlayer = MessengerCharacter(player, player.messengerPosition)
                    world.leaveMessenger(messenger.id, messengerPlayer)
                    player.messenger = null
                    player.messengerPosition = 4
                }
            }
            0x03 -> {
                if (messenger?.members?.size!! < 3) {
                    val input = slea.readGameASCIIString()
                    val target = c.getChannelServer().players.getCharacterByName(input)
                    if (target != null) {
                        if (target.messenger == null) {
                            target.client.announce(InteractPacket.messengerInvite(player.name, messenger.id))
                            c.announce(InteractPacket.messengerNote(input, 4, 1))
                        } else {
                            c.announce(InteractPacket.messengerChat("${player.name} : $input is already using Messenger"))
                        }
                    } else {
                        if (world.findChannelIdByCharacterName(input) > -1) {
                            world.messengerInvite(player.name, messenger.id, input, c.channel)
                        } else {
                            c.announce(InteractPacket.messengerNote(input, 4, 0))
                        }
                    }
                } else {
                    c.announce(InteractPacket.messengerChat("${player.name} : You cannot have more than 3 people in the Messenger"))
                }
            }
            0x05 -> {
                val targeted = slea.readGameASCIIString()
                val target = c.getChannelServer().players.getCharacterByName(targeted)
                if (target != null) {
                    if (target.messenger != null) {
                        target.client.announce(InteractPacket.messengerNote(player.name, 5, 0))
                    }
                } else {
                    world.declineChat(targeted, player.name)
                }
            }
            0x06 -> {
                if (messenger != null) {
                    val messengerPlayer = MessengerCharacter(player, 0)
                    val input = slea.readGameASCIIString()
                    world.messengerChat(messenger, input, messengerPlayer.name)
                }
            }
        }
    }
}