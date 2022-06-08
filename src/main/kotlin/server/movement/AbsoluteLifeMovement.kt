package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

open class AbsoluteLifeMovement(private val unk: Int, val pixelsPerSecond: Point, type: Byte, position: Point, duration: Int, newState: Byte) : AbstractLifeMovement(type, position, duration, newState) {
    override fun serialize(lew: LittleEndianWriter) {
        lew.write(type)
        lew.writeShort(position.x)
        lew.writeShort(position.y)
        lew.writeShort(pixelsPerSecond.x)
        lew.writeShort(pixelsPerSecond.y)
        lew.writeShort(unk)
        lew.write(newState)
        lew.writeShort(duration)
    }
}