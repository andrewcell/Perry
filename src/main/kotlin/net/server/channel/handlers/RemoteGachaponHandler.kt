package net.server.channel.handlers

import server.ItemInformationProvider.getInventoryType
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import client.Client
import scripting.npc.NPCScriptManager

class RemoteGachaponHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val type = slea.readInt()
        if ((c.player?.getInventory(getInventoryType(type))?.countById(type) ?: 0) < 1) return
        val mode = slea.readInt()
        if (type == 5451000) {
            var npcId = 9100100
            if (mode != 8 && mode != 9) {
                npcId += mode
            } else {
                npcId = if (mode == 8) 9100109 else 9100117
            }
            NPCScriptManager.start(c, npcId, null, null)
        }
    }
}