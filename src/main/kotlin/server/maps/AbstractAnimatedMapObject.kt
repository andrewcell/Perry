package server.maps

import kotlin.math.abs

abstract class AbstractAnimatedMapObject : AbstractMapObject(), AnimatedMapObject {
    override var stance: Int = 0

    override fun isFacingLeft() = abs(stance) % 2 == 1
}