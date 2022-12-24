package tools.packet

import client.Client
import constants.ServerConstants
import net.SendPacketOpcode
import net.server.channel.Channel
import tools.HexTool
import tools.PacketCreator
import tools.PacketCreator.Companion.packetWriter
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
            return packetWriter {
                opcode(SendPacketOpcode.CHANGE_CHANNEL)
                byte(1)
                byte(inetAddress.address)
                short(port)
            }
        }

        /**
         * Gets a packet saying that the server list is over.
         *
         * @return The end of server list packet.
         */
        fun getEndOfServerList() = packetWriter(SendPacketOpcode.SERVERLIST) {
            byte(0xFF)
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
            return packetWriter {
                var ret = 0
                ret = ret xor (gameVersion.toInt() and 0x7FFF)
                ret = ret xor (0x01 shl 15)
                ret = ret xor (ServerConstants.patchVersion.toInt() and 0xFF shl 16)
                val version = ret.toString()
                val packetSize = 13 + version.length
                short(packetSize)
                short(291) // KMS Static
                gameASCIIString(version)
                byte(receiveIv)
                byte(sendIv)
                byte(1) // 1 = KMS, 2 = KMST, 7 = MSEA, 8 = GlobalMS, 5 = Test Server
            }
        }

        /**
         * Gets a successful authentication and PIN Request packet.
         *
         * @return The PIN request packet.
         */
        fun getAuthSuccess(c: Client) = packetWriter(SendPacketOpcode.LOGIN_STATUS) {
            byte(0)
            int(c.accountId)
            byte(c.gender)
            bool(c.gmLevel > 0)
            //lew.byte(0);
            gameASCIIString("nxid") // NX ID
            int(0) //v70
            short(0) //value
            byte(0) //a2
            byte(HexTool.getByteArrayFromHexString("28 26 45 2A B2 9D 01")) //really create date v57
            int(0) //??
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
        fun getLoginFailed(reason: Int) = packetWriter {
            opcode(SendPacketOpcode.LOGIN_STATUS)
            byte(reason)
            byte(0)
            int(0)
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
        fun getAfterLoginError(reason: Int) = packetWriter { //same as above o.o
            opcode(SendPacketOpcode.SELECT_CHARACTER_BY_VAC)
            short(reason) //using other types then stated above = CRASH
        }

        fun getPermBan(reason: Byte) = packetWriter {
            opcode(SendPacketOpcode.LOGIN_STATUS)
            byte(2) // Account is banned
            byte(0)
            int(0)
            byte(0)
            long(PacketCreator.getTime(-1))
        }

        /**
         * Sends a ping packet.
         *
         * @return The packet.
         */
        fun getPing() = packetWriter {
            byte(SendPacketOpcode.PING.value)
        }

        /**
         * Gets a packet telling the client the IP of the channel server.
         *
         * @param inetAddress The InetAddress of the requested channel server.
         * @param port     The port the channel is on.
         * @param clientId The ID of the client.
         * @return The server IP packet.
         */
        fun getServerIP(inetAddress: InetAddress, port: Int, clientId: Int) = packetWriter {
            opcode(SendPacketOpcode.SERVER_IP)
            short(0)
            byte(inetAddress.address)
            short(port)
            int(clientId)
            skip(5)
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
        ) = packetWriter {
            opcode(SendPacketOpcode.SERVERLIST)
            byte(serverId)
            gameASCIIString(serverName)
            byte(flag)
            gameASCIIString(eventMessage)
            short(100)
            short(100)
            byte(channelLoad.size)
            for (ch in channelLoad) {
                gameASCIIString("${serverName}-${ch.channelId}")
                int(ch.getConnectedClients() * 1000 / ServerConstants.channelLoad)
                byte(1)
                short(ch.channelId - 1)
            }
            short(0)
        }

        fun getTempBan(timestampTill: Long, reason: Byte) = packetWriter {
            opcode(SendPacketOpcode.LOGIN_STATUS)
            byte(2)
            byte(0)
            int(0)
            byte(reason)
            long(PacketCreator.getTime(timestampTill)) // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.
        }
    }
}