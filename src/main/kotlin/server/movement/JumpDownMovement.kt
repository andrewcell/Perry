package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

class JumpDownMovement(private val pixelsPerSecond: Point, private val unk: Int, val fh: Int, type: Byte, position: Point, duration: Int, newState: Byte) : AbstractLifeMovement(type, position, duration, newState) {
    override fun serialize(lew: LittleEndianWriter) {
        lew.write(type)
        lew.writeShort(position.x)
        lew.writeShort(position.y)
        lew.writeShort(pixelsPerSecond.x)
        lew.writeShort(pixelsPerSecond.y)
        lew.writeShort(unk)
        lew.writeShort(fh)
        lew.write(newState)
        lew.writeShort(duration)
    }
}