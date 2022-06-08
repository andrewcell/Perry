package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import server.ItemInformationProvider.getInventoryType
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class FaceExpressionHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val emote = slea.readInt()
        c.player?.let { player ->
            if (emote > 7) {
                val emoteId = 5159992 + emote
                if (player.getInventory(getInventoryType(emoteId))?.findById(emoteId) == null) {
                    return
                }
            }
            player.map.broadcastMessage(player, CharacterPacket.facialExpression(player, emote), false)
        }
    }
}