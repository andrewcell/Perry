package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import scripting.npc.NPCScriptManager
import server.life.Npc
import server.maps.PlayerNPCs
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor

class NPCTalkHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        if (c.player?.isAlive() == false) {
            c.announce(PacketCreator.enableActions())
            return
        }
        val oid = slea.readInt()
        val map = c.player?.map
        val obj = map?.mapObjects?.get(oid)
        if (obj is Npc) {
            if (c.player?.conversation != 0) return
            if (obj.id == 9010009) {
                c.announce(PacketCreator.sendDuey(8, DueyHandler.loadItems(c.player)))
            } else if (obj.hasShop()) {
                if (c.player?.shop != null) return
                c.player?.conversation = 1
                obj.sendShop(c)
            } else {
                if (c.getCM() != null || c.getQM() != null) {
                    c.announce(PacketCreator.enableActions())
                    return
                }
                NPCScriptManager.start(c, obj.id, null, null)
            }
        } else if (obj is PlayerNPCs) {
            NPCScriptManager.start(c, obj.npcId, null, null)
        }
    }
}