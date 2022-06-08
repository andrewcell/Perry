package net.server

data class PlayerCoolDownValueHolder(
    val skillId: Int,
    val startTime: Long,
    val length: Long
)