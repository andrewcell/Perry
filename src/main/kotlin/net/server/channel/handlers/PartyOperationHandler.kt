package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import net.server.world.PartyCharacter
import net.server.world.PartyOperation
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket

class PartyOperationHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val operation = slea.readByte()
        val player = c.player
        val world = c.getWorldServer()
        var party = player?.party
        var partyPlayer = player?.mpc
        when (operation.toInt()) {
            1 -> { // Create
                if (player?.party == null) {
                    partyPlayer = PartyCharacter(player)
                    party = world.createParty(partyPlayer)
                    player?.party = party
                    player?.mpc = partyPlayer
                    c.announce(InteractPacket.partyCreated(party.id))
                } else {
                    c.announce(InteractPacket.serverNotice(5, "You can't create a party as you are already in one."))
                }
            }
            2 -> {
                if (party != null && partyPlayer != null) {
                    if (partyPlayer == party.leader) {
                        world.updateParty(party.id, PartyOperation.DISBAND, partyPlayer)
                        player?.eventInstance?.disbandParty()
                    } else {
                        world.updateParty(party.id, PartyOperation.LEAVE, partyPlayer)
                        player?.eventInstance?.leftParty(player)
                    }
                    player?.party = null
                }
            }
            3 -> { // Join
                val partyId = slea.readInt()
                if (c.player?.party == null) {
                    party = world.getParty(partyId)
                    if (party != null) {
                        if (party.members.size < 6) {
                            partyPlayer = PartyCharacter(player)
                            world.updateParty(party.id, PartyOperation.JOIN, partyPlayer)
                            player?.receivePartyMemberHp()
                            player?.updatePartyMemberHp()
                        } else {
                            c.announce(InteractPacket.partyStatusMessage(14))
                        }
                    } else {
                        c.announce(InteractPacket.serverNotice(5, "The person you have invited to the party is already in one."))
                    }
                } else {
                    c.announce(InteractPacket.serverNotice(5, "You can't join the party as you are already in one."))
                }
            }
            4 -> { // Invite
                val cid = slea.readInt()
                val invited = world.players.getCharacterById(cid)
                if (invited != null) {
                    if (invited.party == null) {
                        if ((party?.members?.size ?: 6) < 6) {
                            invited.client.announce(InteractPacket.partyInvite(player))
                        } else {
                            c.announce(InteractPacket.partyStatusMessage(14))
                        }
                    } else {
                        c.announce(InteractPacket.partyStatusMessage(15))
                    }
                } else {
                    c.announce(InteractPacket.partyStatusMessage(17))
                }
            }
            5 -> { // Expel
                val cid = slea.readInt()
                party?.let {
                    if (partyPlayer == party.leader) {
                        val expelled = party.getMemberById(cid)
                        if (expelled != null) {
                            world.updateParty(party.id, PartyOperation.EXPEL, expelled)
                            if (expelled.online) player?.eventInstance?.disbandParty()
                        }
                    }
                }
            }
            6 -> {
                val newLeaderId = slea.readInt()
                val newLeader = party?.getMemberById(newLeaderId) ?: return
                party.leader = newLeader
                world.updateParty(party.id, PartyOperation.CHANGE_LEADER, newLeader)
            }
        }
    }
}