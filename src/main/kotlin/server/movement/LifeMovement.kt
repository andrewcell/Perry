package server.movement

interface LifeMovement : LifeMovementFragment{
    val newState: Byte
    val duration: Int
    val type: Byte
}