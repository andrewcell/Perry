package net.server.channel.handlers

import net.AbstractPacketHandler
import tools.data.input.LittleEndianAccessor
import server.movement.LifeMovementFragment
import server.movement.AbsoluteLifeMovement
import server.movement.RelativeLifeMovement
import server.movement.TeleportMovement
import server.movement.JumpDownMovement
import server.maps.AnimatedMapObject
import server.movement.LifeMovement
import java.awt.Point
import java.util.ArrayList

abstract class AbstractMovementPacketHandler : AbstractPacketHandler() {
    protected fun parseMovement(lea: LittleEndianAccessor): List<LifeMovementFragment> {
        val res: MutableList<LifeMovementFragment> = ArrayList()
        val numCommands = lea.readByte()
        for (i in 0 until numCommands) {
            when (val command = lea.readByte().toInt()) {
                0, 5, 15, 17 -> {
                    // Float
                    val xPos = lea.readShort()
                    val yPos = lea.readShort()
                    val xWobble = lea.readShort()
                    val yWobble = lea.readShort()
                    val unk = lea.readShort()
                    val newState = lea.readByte()
                    val duration = lea.readShort()
                    val alm = AbsoluteLifeMovement(
                        unk.toInt(), Point(xWobble.toInt(), yWobble.toInt()), command.toByte(), Point(
                            xPos.toInt(), yPos.toInt()), duration.toInt(), newState)
                    res.add(alm)
                }
                1, 2, 6, 12, 13, 16 -> {
                    // Float
                    val xPos = lea.readShort()
                    val yPos = lea.readShort()
                    val newState = lea.readByte()
                    val duration = lea.readShort()
                    val rlm = RelativeLifeMovement(command.toByte(), Point(xPos.toInt(), yPos.toInt()), duration.toInt(), newState)
                    res.add(rlm)
                }
                3, 4, 7, 8, 10, 11 -> {
                    val xPos = lea.readShort()
                    val yPos = lea.readShort()
                    val xWobble = lea.readShort()
                    val yWobble = lea.readShort()
                    val newState = lea.readByte()
                    val tm = TeleportMovement(
                        Point(xWobble.toInt(), yWobble.toInt()),
                        command.toByte(), Point(xPos.toInt(), yPos.toInt()), newState)
                    res.add(tm)
                }
                9 -> {
                    // Change Equip
                    lea.readByte()
                }
                14 -> {
                    val xPos = lea.readShort()
                    val yPos = lea.readShort()
                    val xWobble = lea.readShort()
                    val yWobble = lea.readShort()
                    val unk = lea.readShort()
                    val fh = lea.readShort()
                    val newState = lea.readByte()
                    val duration = lea.readShort()
                    val jdm = JumpDownMovement(Point(xWobble.toInt(), yWobble.toInt()), unk.toInt(), fh.toInt(), command.toByte(), Point(xPos.toInt(), yPos.toInt()), duration.toInt(), newState)
                    res.add(jdm)
                }
                else -> return emptyList()
            }
        }
        return res
    }

    protected fun updatePosition(movement: List<LifeMovementFragment?>, target: AnimatedMapObject, yOffset: Int) {
        for (move in movement) {
            if (move is LifeMovement) {
                if (move is AbsoluteLifeMovement) {
                    val position = (move as LifeMovement).position
                    position.y += yOffset
                    target.position = position
                }
                target.stance = move.newState.toInt()
            }
        }
    }
}