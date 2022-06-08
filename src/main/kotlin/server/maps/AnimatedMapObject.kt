package server.maps

interface AnimatedMapObject : MapObject {
    var stance: Int
    fun isFacingLeft(): Boolean
}