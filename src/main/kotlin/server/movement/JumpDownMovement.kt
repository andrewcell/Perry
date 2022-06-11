package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

class JumpDownMovement(private val pixelsPerSecond: Point, private val unk: Int, val fh: Int, type: Byte, position: Point, duration: Int, newState: Byte) : AbstractLifeMovement(type, position, duration, newState) {
    override fun serialize(lew: LittleEndianWriter) {
        lew.byte(type)
        lew.short(position.x)
        lew.short(position.y)
        lew.short(pixelsPerSecond.x)
        lew.short(pixelsPerSecond.y)
        lew.short(unk)
        lew.short(fh)
        lew.byte(newState)
        lew.short(duration)
    }
}