package gm

enum class GMRecvOpcode(val value: Int = -2) {
    LOGIN(0x00),
    GM_CHAT(0x01),
    PLAYER_LIST(0x02),
    COMMAND(0x03),
}