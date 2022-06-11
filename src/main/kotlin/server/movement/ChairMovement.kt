package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

class ChairMovement(private val unk: Int, type: Byte, position: Point, duration: Int, newState: Byte) : AbstractLifeMovement(type, position, duration, newState){
    override fun serialize(lew: LittleEndianWriter) {
        lew.byte(type)
        lew.short(position.x)
        lew.short(position.y)
        lew.short(unk)
        lew.byte(newState)
        lew.short(duration)
    }
}