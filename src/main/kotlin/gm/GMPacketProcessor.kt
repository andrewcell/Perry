package gm

import gm.server.handler.ChatHandler
import gm.server.handler.CommandHandler
import gm.server.handler.LoginHandler
import gm.server.handler.PlayerListHandler

class GMPacketProcessor {
    var handlers = arrayOfNulls<GMPacketHandler>(GMRecvOpcode.values().maxOf { it.value } + 1)

    init {
        reset()
    }

    fun getHandler(packetId: Short): GMPacketHandler? {
        if (packetId > handlers.size) return null
        return handlers[packetId.toInt()]
    }

    fun registerHandler(code: GMRecvOpcode, handler: GMPacketHandler) {
        try {
            handlers[code.value] = handler
        } catch (e: Exception) {

        }
    }

    fun reset() {
        handlers = arrayOfNulls(handlers.size)
        registerHandler(GMRecvOpcode.LOGIN, LoginHandler())
        registerHandler(GMRecvOpcode.GM_CHAT, ChatHandler())
        registerHandler(GMRecvOpcode.PLAYER_LIST, PlayerListHandler())
        registerHandler(GMRecvOpcode.COMMAND, CommandHandler())
    }
}