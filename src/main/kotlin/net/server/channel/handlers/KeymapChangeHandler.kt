package net.server.channel.handlers

import client.SkillFactory.Companion.getSkill
import client.Client
import client.KeyBinding
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class KeymapChangeHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val player = c.player ?: return
        when (slea.readInt()) {
            0 -> {
                if (slea.available() != 8L) {
                    val numChanges = slea.readInt()
                    var i = 0
                    while (i < numChanges) {
                        val key = slea.readInt()
                        val type = slea.readByte().toInt()
                        val action = slea.readInt()
                        val skill = getSkill(action)
                        if (type != 2 && skill != null && player.getSkillLevel(skill) < 1) {
                            i++
                            continue
                        }
                        player.changeKeyBinding(key, KeyBinding(type, action))
                        i++
                    }
                }
            }
            1 -> {
                val itemId = slea.readInt()
                player.petAutoHp = itemId
            }
        }
    }
}