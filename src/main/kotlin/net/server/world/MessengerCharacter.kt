package net.server.world

import client.Character

data class MessengerCharacter(val c: Character, var position: Int = 0) {
    val name = c.name
    val channel = c.client.channel
    val id = c.id
    var online = true
}