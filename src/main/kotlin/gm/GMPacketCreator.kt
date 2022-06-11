package gm

import tools.data.output.PacketLittleEndianWriter

class GMPacketCreator {
    companion object {
        fun keyResponse(ok: Boolean): ByteArray {
            val mplew = PacketLittleEndianWriter(3)
            mplew.short(GMSendOpcode.LOGIN_RESPONSE.value)
            mplew.byte(if (ok) 1 else 0)
            return mplew.getPacket()
        }

        fun sendLoginResponse(loginOk: Byte, login: String): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.short(GMSendOpcode.LOGIN_RESPONSE.value)
            mplew.byte(loginOk)
            if (loginOk.toInt() == 3) {
                mplew.gameASCIIString(login)
            }
            return mplew.getPacket()
        }

        fun chat(msg: String): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.short(GMSendOpcode.CHAT.value)
            mplew.gameASCIIString(msg)
            return mplew.getPacket()
        }

        fun sendUserList(names: List<String>): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.short(GMSendOpcode.GM_LIST.value)
            mplew.byte(0)
            for (name in names) {
                mplew.gameASCIIString(name)
            }
            return mplew.getPacket()
        }

        fun addUser(name: String): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.short(GMSendOpcode.GM_LIST.value)
            mplew.byte(1)
            mplew.gameASCIIString(name)
            return mplew.getPacket()
        }

        fun removeUser(name: String): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.short(GMSendOpcode.GM_LIST.value)
            mplew.byte(2)
            mplew.gameASCIIString(name)
            return mplew.getPacket()
        }

        fun sendPlayerList(list: List<String>): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.short(GMSendOpcode.SEND_PLAYER_LIST.value)
            for (s in list) {
                mplew.gameASCIIString(s)
            }
            return mplew.getPacket()
        }

        fun commandResponse(op: Byte): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.short(GMSendOpcode.COMMAND_RESPONSE.value)
            mplew.byte(op)
            return mplew.getPacket()
        }

        fun playerStats(
            name: String, job: String, level: Byte,
            exp: Int, hp: Short,
            mp: Short, str: Short, dex: Short,
            int: Short, luk: Short, meso: Int
        ): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.short(GMSendOpcode.COMMAND_RESPONSE.value)
            mplew.byte(3)
            mplew.gameASCIIString(name)
            mplew.gameASCIIString(job)
            mplew.byte(level)
            mplew.int(exp)
            mplew.short(hp.toInt())
            mplew.short(mp.toInt())
            mplew.short(str.toInt())
            mplew.short(dex.toInt())
            mplew.short(int.toInt())
            mplew.short(luk.toInt())
            mplew.int(meso)
            return mplew.getPacket()
        }
    }
}