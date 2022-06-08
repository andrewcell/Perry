package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

class TeleportMovement(pixelsPerSecond: Point, type: Byte, position: Point, newState: Byte) : AbsoluteLifeMovement(0, pixelsPerSecond, type, position, 0, newState) {
    override fun serialize(lew: LittleEndianWriter) {
        lew.write(type)
        lew.writeShort(position.x)
        lew.writeShort(position.y)
        lew.writeShort(pixelsPerSecond.x)
        lew.writeShort(pixelsPerSecond.y)
        lew.write(newState)
    }
}