package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class MobDamageMobHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val from = slea.readInt()
        slea.readInt()
        val to = slea.readInt()
        slea.readByte()
        val dmg = slea.readInt()
        val map = c.player?.map ?: return
        val mob = map.getMonsterByOid(to)
        if (map.getMonsterByOid(from) != null && mob != null) {
            c.player?.let { map.damageMonster(it, mob, dmg) }
        }
    }
}