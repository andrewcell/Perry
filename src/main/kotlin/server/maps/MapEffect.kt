package server.maps

import client.Client
import tools.packet.GameplayPacket

data class MapEffect(val msg: String, val itemId: Int) {
    private var active = true

    fun makeDestroyData(): ByteArray = GameplayPacket.removeMapEffect()

    fun makeStartData(): ByteArray = GameplayPacket.startMapEffect(msg, itemId, active)

    fun sendStartData(client: Client) = client.announce(makeStartData())
}