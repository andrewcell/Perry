package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.GameplayPacket
import kotlin.random.Random

class MobDamageMobFriendlyHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val attacker = slea.readInt()
        slea.readInt() //charId
        val damaged = slea.readInt()
        c.player?.let { player ->
            val damage = Random.nextInt(
                ((player.map.getMonsterByOid(damaged)?.getMaxHp()
                    ?: 1) / 13 + (player.map.getMonsterByOid(attacker)?.stats?.paDamage ?: 1) * 10) * 2 + 500
            ) // Beng formula.
            if (player.map.getMonsterByOid(damaged) == null || player.map.getMonsterByOid(attacker) == null) {
                return
            }
            player.map.getMonsterByOid(damaged)?.let {
                player.map.broadcastMessage(
                    GameplayPacket.mobDamageMobFriendly(it, damage), it.position)
            }
            c.announce(PacketCreator.enableActions())
        }
    }
}