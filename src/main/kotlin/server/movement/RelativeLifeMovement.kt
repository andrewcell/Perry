package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

class RelativeLifeMovement(type: Byte, position: Point, duration: Int, override val newState: Byte) : AbstractLifeMovement(type, position, duration, newState) {
    override fun serialize(lew: LittleEndianWriter) {
        lew.byte(type)
        lew.short(position.x)
        lew.short(position.y)
        lew.byte(newState)
        lew.short(duration)
    }
}