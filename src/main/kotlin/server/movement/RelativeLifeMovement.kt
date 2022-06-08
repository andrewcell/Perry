package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

class RelativeLifeMovement(type: Byte, position: Point, duration: Int, override val newState: Byte) : AbstractLifeMovement(type, position, duration, newState) {
    override fun serialize(lew: LittleEndianWriter) {
        lew.write(type)
        lew.writeShort(position.x)
        lew.writeShort(position.y)
        lew.write(newState)
        lew.writeShort(duration)
    }
}