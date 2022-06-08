package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

interface LifeMovementFragment {
    fun serialize(lew: LittleEndianWriter)
    val position: Point
}