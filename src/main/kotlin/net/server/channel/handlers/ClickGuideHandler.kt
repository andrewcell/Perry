package net.server.channel.handlers

import client.Client
import scripting.npc.NPCScriptManager.start
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class ClickGuideHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        start(c, 1202000, null, null)
    }
}