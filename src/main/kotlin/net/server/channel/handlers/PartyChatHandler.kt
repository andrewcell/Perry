package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import net.server.Server.getGuild
import tools.data.input.SeekableLittleEndianAccessor

class PartyChatHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val player = c.player ?: return
        val type = slea.readByte().toInt() // 0 for buddies, 1 for parties
        val numRecipients = slea.readByte()
        val recipients = IntArray(numRecipients.toInt())
        for (i in 0 until numRecipients) {
            recipients[i] = slea.readInt()
        }
        val chatText = slea.readGameASCIIString()
        val world = c.getWorldServer()
        val party = player.party
        if (type == 0) {
            world.buddyChat(recipients.toTypedArray(), player.id, player.name, chatText)
        } else if (type == 1 && party != null) {
            world.partyChat(party, chatText, player.name)
        } else if (type == 2 && player.guildId > 0) {
            getGuild(player.guildId)?.guildChat(player.name, player.id, chatText)
        }
    }
}