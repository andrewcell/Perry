package net.server.world

import client.Character
import java.awt.Point

data class PartyCharacter(val chr: Character? = null) {
    val name = chr?.name
    val level = chr?.level
    var channel = chr?.client?.channel
    val world = chr?.world
    val id = chr?.id
    val jobId = chr?.id
    var mapId = chr?.mapId
    val job = chr?.job
    var online = true
    val doorTown = chr?.doors?.firstOrNull()?.town?.mapId ?: 999999999
    val doorTarget = chr?.doors?.firstOrNull()?.target?.mapId ?: 999999999
    val doorPosition = chr?.doors?.firstOrNull()?.targetPosition ?: Point(0, 0)
}