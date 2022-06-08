package gm

enum class GMSendOpcode(val value: Int = -2) {
    LOGIN_RESPONSE(0x00),
    CHAT(0x01),
    GM_LIST(0x02),
    SEND_PLAYER_LIST(0x03),
    COMMAND_RESPONSE(0x04);
}