package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import net.SendPacketOpcode
import tools.data.input.SeekableLittleEndianAccessor
import tools.data.output.PacketLittleEndianWriter

class NPCAnimation : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val lew = PacketLittleEndianWriter()
        val length = slea.available().toInt()
        if (length == 6) { // NPC talk
            lew.byte(SendPacketOpcode.NPC_ACTION.value)
            lew.int(slea.readInt())
            lew.short(slea.readShort().toInt())
            c.announce(lew.getPacket())
        } else if (length > 6) { // NPC Move
            val bytes = slea.read(length - 9)
            lew.byte(SendPacketOpcode.NPC_ACTION.value)
            lew.byte(bytes)
            c.announce(lew.getPacket())
        }
    }
}