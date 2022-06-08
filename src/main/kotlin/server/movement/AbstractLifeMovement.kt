package server.movement

import java.awt.Point

abstract class AbstractLifeMovement(
    override val type: Byte,
    override val position: Point,
    override val duration: Int,
    override val newState: Byte
) : LifeMovement