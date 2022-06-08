package server.maps

import client.Client
import java.awt.Point

interface MapObject {
    var objectId: Int
    val objectType: MapObjectType
    var position: Point
    fun sendSpawnData(client: Client)
    fun sendDestroyData(client: Client)
}