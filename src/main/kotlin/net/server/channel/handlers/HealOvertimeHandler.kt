package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import client.autoban.AutobanFactory

class HealOvertimeHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        slea.skip(4)
        val healHP = slea.readShort()
        val healMP = slea.readShort()
        if (chr.hp <= 0) return
        if (healHP.toInt() != 0) {
            if (healHP > 140) {
                AutobanFactory.HIGH_HP_HEALING.autoban(chr, "Healing: $healHP; Max is 140.")
                return
            }
            chr.addHp(healHP.toInt())
            chr.checkBerserk()
            return
        }
        if (healMP.toInt() != 0 && healMP < 1000) {
            chr.addMp(healMP.toInt())
        }
    }
}