package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class AutoAggroHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val oid = slea.readInt()
        val monster = c.player?.map?.getMonsterByOid(oid)
        if (monster?.controller != null) {
            if (!monster.controllerHasAggro) {
                if (c.player?.map?.getCharacterById(monster.controller?.get()?.id ?: 0) == null) {
                    c.player?.let { monster.switchController(it, true) }
                } else {
                    monster.controller?.get()?.let { monster.switchController(it, true) }
                }
            } else if (c.player?.map?.getCharacterById(monster.controller?.get()?.id ?: 0) == null) {
                c.player?.let { monster.switchController(it, true) }
            }
        } else if (monster != null && monster.controller == null) {
            monster.switchController(c.player!!, true)
        }
    }
}