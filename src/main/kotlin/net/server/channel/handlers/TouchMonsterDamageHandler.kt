package net.server.channel.handlers

import client.BuffStat
import client.Client
import tools.data.input.SeekableLittleEndianAccessor

class TouchMonsterDamageHandler : AbstractDealDamageHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        if (c.player?.energybar == 15000 || c.player?.getBuffedValue(BuffStat.BODY_PRESSURE) != null) {
            c.player?.let {
                applyAttack(parseDamage(slea, it, false), it, 1)
            }
        }
    }
}