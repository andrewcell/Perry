package net.server

import client.Disease

data class PlayerDiseaseValueHolder(
    val disease: Disease,
    val startTime: Long,
    val length: Long
)