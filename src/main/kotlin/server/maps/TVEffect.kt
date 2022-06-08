package server.maps

import client.Character
import net.server.Server
import tools.CoroutineManager
import tools.PacketCreator

class TVEffect(private val user: Character, private val partner: Character, val message: List<String>, val type: Int, val world: Int) {
    init {
        broadcastTV(true)
    }

    private fun broadcastTV(_active: Boolean) {
        val server = Server
        active = true
        if (_active) {
            server.broadcastMessage(world, PacketCreator.enableTV())
            server.broadcastMessage(world, PacketCreator.sendTV(user, message, if (type <= 2) type else type -3, partner))
            val delay = when (type) {
                4 -> 30000
                5 -> 60000
                else -> 15000
            }
            CoroutineManager.schedule({
                broadcastTV(false)
            }, delay.toLong())
        } else {
            server.broadcastMessage(world, PacketCreator.removeTV())
        }
    }

    companion object {
        var active = false
    }
}