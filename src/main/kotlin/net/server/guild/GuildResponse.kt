package net.server.guild

import tools.packet.GuildPacket

enum class GuildResponse(val value: Int) {
    NOT_IN_CHANNEL(0x2a),
    ALREADY_IN_GUILD(0x28),
    NOT_IN_GUILD(0x2d);

    fun getPacket(): ByteArray = GuildPacket.genericGuildMessage(value.toByte())
}