package server.maps

import java.awt.Point

abstract class AbstractMapObject : MapObject {
    override var objectId: Int = -1
    override var position = Point()
    abstract override val objectType: MapObjectType
}