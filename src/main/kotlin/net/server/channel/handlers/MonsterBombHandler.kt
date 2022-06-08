package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket

class MonsterBombHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val oid = slea.readInt()
        val monster = c.player?.map?.getMonsterByOid(oid)
        if (c.player?.isAlive() != true || monster == null) return
        if (monster.id == 8500003 || monster.id == 8500004) {
            monster.map?.broadcastMessage(GameplayPacket.killMonster(monster.objectId, 4))
            c.player?.map?.removeMapObject(monster)
        }
    }
}