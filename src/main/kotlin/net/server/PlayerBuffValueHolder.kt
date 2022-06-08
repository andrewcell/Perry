package net.server

import server.StatEffect

data class PlayerBuffValueHolder(
    val startTime: Long,
    val effect: StatEffect
)