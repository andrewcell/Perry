package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

class TeleportMovement(pixelsPerSecond: Point, type: Byte, position: Point, newState: Byte) : AbsoluteLifeMovement(0, pixelsPerSecond, type, position, 0, newState) {
    override fun serialize(lew: LittleEndianWriter) {
        lew.byte(type)
        lew.short(position.x)
        lew.short(position.y)
        lew.short(pixelsPerSecond.x)
        lew.short(pixelsPerSecond.y)
        lew.byte(newState)
    }
}