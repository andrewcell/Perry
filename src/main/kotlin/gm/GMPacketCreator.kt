package gm

import tools.data.output.PacketLittleEndianWriter

class GMPacketCreator {
    companion object {
        fun keyResponse(ok: Boolean): ByteArray {
            val mplew = PacketLittleEndianWriter(3)
            mplew.writeShort(GMSendOpcode.LOGIN_RESPONSE.value)
            mplew.write(if (ok) 1 else 0)
            return mplew.getPacket()
        }

        fun sendLoginResponse(loginOk: Byte, login: String): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.writeShort(GMSendOpcode.LOGIN_RESPONSE.value)
            mplew.write(loginOk)
            if (loginOk.toInt() == 3) {
                mplew.writeGameASCIIString(login)
            }
            return mplew.getPacket()
        }

        fun chat(msg: String): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.writeShort(GMSendOpcode.CHAT.value)
            mplew.writeGameASCIIString(msg)
            return mplew.getPacket()
        }

        fun sendUserList(names: List<String>): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.writeShort(GMSendOpcode.GM_LIST.value)
            mplew.write(0)
            for (name in names) {
                mplew.writeGameASCIIString(name)
            }
            return mplew.getPacket()
        }

        fun addUser(name: String): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.writeShort(GMSendOpcode.GM_LIST.value)
            mplew.write(1)
            mplew.writeGameASCIIString(name)
            return mplew.getPacket()
        }

        fun removeUser(name: String): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.writeShort(GMSendOpcode.GM_LIST.value)
            mplew.write(2)
            mplew.writeGameASCIIString(name)
            return mplew.getPacket()
        }

        fun sendPlayerList(list: List<String>): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.writeShort(GMSendOpcode.SEND_PLAYER_LIST.value)
            for (s in list) {
                mplew.writeGameASCIIString(s)
            }
            return mplew.getPacket()
        }

        fun commandResponse(op: Byte): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.writeShort(GMSendOpcode.COMMAND_RESPONSE.value)
            mplew.write(op)
            return mplew.getPacket()
        }

        fun playerStats(
            name: String, job: String, level: Byte,
            exp: Int, hp: Short,
            mp: Short, str: Short, dex: Short,
            int: Short, luk: Short, meso: Int
        ): ByteArray {
            val mplew = PacketLittleEndianWriter()
            mplew.writeShort(GMSendOpcode.COMMAND_RESPONSE.value)
            mplew.write(3)
            mplew.writeGameASCIIString(name)
            mplew.writeGameASCIIString(job)
            mplew.write(level)
            mplew.writeInt(exp)
            mplew.writeShort(hp.toInt())
            mplew.writeShort(mp.toInt())
            mplew.writeShort(str.toInt())
            mplew.writeShort(dex.toInt())
            mplew.writeShort(int.toInt())
            mplew.writeShort(luk.toInt())
            mplew.writeInt(meso)
            return mplew.getPacket()
        }
    }
}