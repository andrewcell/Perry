package server.movement

import tools.data.output.LittleEndianWriter
import java.awt.Point

class ChangeEquip(private val wui: Int) : LifeMovementFragment{
    override fun serialize(lew: LittleEndianWriter) {
        lew.write(10)
        lew.write(wui)
    }

    override val position = Point(0, 0)
}