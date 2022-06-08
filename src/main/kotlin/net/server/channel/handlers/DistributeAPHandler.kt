package net.server.channel.handlers

import client.Character
import client.CharacterStat
import client.Client
import client.GameJob
import mu.KLogging
import net.AbstractPacketHandler
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import kotlin.math.min

class DistributeAPHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val num = slea.readInt()
        c.player?.let { player ->
            if (player.remainingAp > 0) {
                if (addStat(c, num)) {
                    player.remainingAp = player.remainingAp - 1
                    player.updateSingleStat(CharacterStat.AVAILABLEAP, player.remainingAp, false)
                }
            }
            c.announce(PacketCreator.enableActions())
        }
    }

    companion object : KLogging() {
        const val max = 999

        fun addStat(c: Client, id: Int): Boolean {
            c.player?.let { player ->
                when (id) {
                    64 -> { // STR
                        if (player.str >= max) return false
                        player.addStat(1, 1)
                    }
                    128 -> { // DEX
                        if (player.dex >= max) return false
                        player.addStat(2, 1)
                    }
                    256 -> { // INT
                        if (player.int >= max) return false
                        player.addStat(3, 1)
                    }
                    512 -> { // LUK
                        if (player.luk >= max) return false
                        player.addStat(4, 1)
                    }
                    2048 -> { // HP
                        addHp(player, addHp(c))
                    }
                    8192 -> { // MP
                        addMp(player, addMp(c))
                    }
                    else -> {
                        c.announce(CharacterPacket.updatePlayerStats(PacketCreator.EMPTY_STATUPDATE, true))
                        return false
                    }
                }
                return true
            }
            return false
        }

        private fun addHp(c: Client): Int {
            c.player?.let {
                val job = it.job
                var maxHp = it.maxHp
                if (it.hpMpApUsed > 9999 || maxHp >= 30000) return maxHp
                maxHp += when {
                    job.isA(GameJob.WARRIOR) -> 20
                    job.isA(GameJob.MAGICIAN) -> 6
                    job.isA(GameJob.BOWMAN) -> 16
                    job.isA(GameJob.PIRATE) -> 18
                    else -> 8
                }
                return maxHp
            }
            return 1
        }

        private fun addMp(c: Client): Int {
            c.player?.let {
                val job = it.job
                var maxMp = it.maxMp
                if (it.hpMpApUsed > 9999 || it.maxMp >= 30000) return maxMp
                maxMp += when {
                    job.isA(GameJob.WARRIOR) -> 2
                    job.isA(GameJob.MAGICIAN) -> 18
                    job.isA(GameJob.BOWMAN) || job.isA(GameJob.THIEF) -> 10
                    job.isA(GameJob.PIRATE) -> 14
                    else -> 6
                }
                return maxMp
            }
            return 1
        }

        private fun addHp(player: Character, maxHp: Int) {
            val hp = min(30000, maxHp)
            player.hpMpApUsed++
            player.maxHp = hp
            player.updateSingleStat(CharacterStat.MAXHP, hp, false)
        }

        private fun addMp(player: Character, maxMp: Int) {
            val mp = min(30000, maxMp)
            player.hpMpApUsed++
            player.maxMp = mp
            player.updateSingleStat(CharacterStat.MAXMP, mp, false)
        }
    }
}