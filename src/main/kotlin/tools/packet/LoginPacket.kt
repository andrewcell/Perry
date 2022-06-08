package tools.packet

import client.Client
import constants.ServerConstants
import net.SendPacketOpcode
import net.server.channel.Channel
import tools.HexTool
import tools.PacketCreator
import tools.data.output.PacketLittleEndianWriter
import java.net.InetAddress

class LoginPacket {
    companion object {
        /**
         * Gets a packet telling the client the IP of the new channel.
         *
         * @param inetAddress The InetAddress of the requested channel server.
         * @param port     The port the channel is on.
         * @return The server IP packet.
         */
        fun getChannelChange(inetAddress: InetAddress, port: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.CHANGE_CHANNEL.value)
            lew.write(1)
            val address = inetAddress.address
            lew.write(address)
            lew.writeShort(port)
            return lew.getPacket()
        }

        /**
         * Gets a packet saying that the server list is over.
         *
         * @return The end of server list packet.
         */
        fun getEndOfServerList(): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SERVERLIST.value)
            lew.write(0xFF)
            return lew.getPacket()
        }

        /**
         * Sends a hello packet.
         *
         * @param gameVersion The game client version.
         * @param sendIv       the IV used by the server for sending
         * @param receiveIv       the IV used by the server for receiving
         * @return
         */
        fun getHello(gameVersion: Short, sendIv: ByteArray, receiveIv: ByteArray): ByteArray {
            val lew = PacketLittleEndianWriter()
            var ret = 0
            ret = ret xor (gameVersion.toInt() and 0x7FFF)
            ret = ret xor (0x01 shl 15)
            ret = ret xor (ServerConstants.patchVersion.toInt() and 0xFF shl 16)
            val version = ret.toString()
            val packetSize = 13 + version.length
            lew.writeShort(packetSize)
            lew.writeShort(291) //KMS Static
            lew.writeGameASCIIString(version)
            lew.write(receiveIv)
            lew.write(sendIv)
            lew.write(1) // 1 = KMS, 2 = KMST, 7 = MSEA, 8 = GlobalMS, 5 = Test Server
            return lew.getPacket()
        }

        /**
         * Gets a successful authentication and PIN Request packet.
         *
         * @return The PIN request packet.
         */
        fun getAuthSuccess(c: Client): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.LOGIN_STATUS.value)
            lew.write(0)
            lew.writeInt(c.accountId)
            lew.write(c.gender)
            lew.writeBool(c.gmLevel > 0)
            //lew.write(0);
            lew.writeGameASCIIString("nxid") // NX ID
            lew.writeInt(0) //v70
            lew.writeShort(0) //value
            lew.write(0) //a2
            lew.write(HexTool.getByteArrayFromHexString("28 26 45 2A B2 9D 01")) //really create date v57
            lew.writeInt(0) //??
            return lew.getPacket()
        }

        /**
         * Gets a login failed packet.
         *
         *
         * Possible values for
         * `reason`:<br></br> 3: ID deleted or blocked<br></br> 4: Incorrect
         * password<br></br> 5: Not a registered id<br></br> 6: System error<br></br> 7: Already
         * logged in<br></br> 8: System error<br></br> 9: System error<br></br> 10: Cannot process
         * so many connections<br></br> 11: Only users older than 20 can use this
         * channel<br></br> 13: Unable to log on as master at this ip<br></br> 14: Wrong
         * gateway or personal info and weird korean button<br></br> 15: Processing
         * request with that korean button!<br></br> 16: Please verify your account
         * through email...<br></br> 17: Wrong gateway or personal info<br></br> 21: Please
         * verify your account through email...<br></br> 23: License agreement<br></br> 25:
         * Europe notice<br></br> 27: Some weird full client
         * notice, probably for trial versions<br></br>
         *
         * @param reason The reason logging in failed.
         * @return The login failed packet.
         */
        fun getLoginFailed(reason: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.LOGIN_STATUS.value)
            lew.write(reason)
            lew.write(0)
            lew.writeInt(0)
            return lew.getPacket()
        }

        /**
         * Gets a login failed packet.
         *
         *
         * Possible values for
         * `reason`:<br></br> 2: ID deleted or blocked<br></br> 3: ID deleted or
         * blocked<br></br> 4: Incorrect password<br></br> 5: Not a registered id<br></br> 6:
         * Trouble logging into the game?<br></br> 7: Already logged in<br></br> 8: Trouble
         * logging into the game?<br></br> 9: Trouble logging into the game?<br></br> 10:
         * Cannot process so many connections<br></br> 11: Only users older than 20 can
         * use this channel<br></br> 12: Trouble logging into the game?<br></br> 13: Unable to
         * log on as master at this ip<br></br> 14: Wrong gateway or personal info and
         * weird korean button<br></br> 15: Processing request with that korean
         * button!<br></br> 16: Please verify your account through email...<br></br> 17: Wrong
         * gateway or personal info<br></br> 21: Please verify your account through
         * email...<br></br> 23: Crashes<br></br> 25: Game Europe notice
         * <br></br> 27: Some weird full client notice, probably for trial
         * versions<br></br>
         *
         * @param reason The reason logging in failed.
         * @return The login failed packet.
         */
        fun getAfterLoginError(reason: Int): ByteArray { //same as above o.o
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SELECT_CHARACTER_BY_VAC.value)
            lew.writeShort(reason) //using other types then stated above = CRASH
            return lew.getPacket()
        }

        fun getPermBan(reason: Byte): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.LOGIN_STATUS.value)
            lew.write(2) // Account is banned
            lew.write(0)
            lew.writeInt(0)
            lew.write(0)
            lew.writeLong(PacketCreator.getTime(-1))
            return lew.getPacket()
        }

        /**
         * Sends a ping packet.
         *
         * @return The packet.
         */
        fun getPing(): ByteArray {
            val plew = PacketLittleEndianWriter()
            plew.write(SendPacketOpcode.PING.value)
            return plew.getPacket()
        }

        /**
         * Gets a packet telling the client the IP of the channel server.
         *
         * @param inetAddress The InetAddress of the requested channel server.
         * @param port     The port the channel is on.
         * @param clientId The ID of the client.
         * @return The server IP packet.
         */
        fun getServerIP(inetAddress: InetAddress, port: Int, clientId: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SERVER_IP.value)
            lew.writeShort(0)
            val address = inetAddress.address
            lew.write(address)
            lew.writeShort(port)
            lew.writeInt(clientId)
            lew.skip(5)
            return lew.getPacket()
        }

        /**
         * Gets a packet detailing a server and its channels.
         *
         * @param serverId
         * @param serverName  The name of the server.
         * @param channelLoad Load of the channel - 1200 seems to be max.
         * @return The server info packet.
         */
        fun getServerList(
            serverId: Int,
            serverName: String,
            flag: Int,
            eventMessage: String,
            channelLoad: List<Channel>
        ): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SERVERLIST.value)
            lew.write(serverId)
            lew.writeGameASCIIString(serverName)
            lew.write(flag)
            lew.writeGameASCIIString(eventMessage)
            lew.writeShort(100)
            lew.writeShort(100)
            lew.write(channelLoad.size)
            for (ch in channelLoad) {
                lew.writeGameASCIIString("${serverName}-${ch.channelId}")
                lew.writeInt(ch.getConnectedClients() * 1000 / ServerConstants.channelLoad)
                lew.write(1)
                lew.writeShort(ch.channelId - 1)
            }
            lew.writeShort(0)
            return lew.getPacket()
        }

        fun getTempBan(timestampTill: Long, reason: Byte): ByteArray {
            val lew = PacketLittleEndianWriter(17)
            lew.write(SendPacketOpcode.LOGIN_STATUS.value)
            lew.write(2)
            lew.write(0)
            lew.writeInt(0)
            lew.write(reason)
            lew.writeLong(PacketCreator.getTime(timestampTill)) // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.
            return lew.getPacket()
        }
    }
}