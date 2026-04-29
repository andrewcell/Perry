package net

import client.Client
import tools.data.input.SeekableLittleEndianAccessor

/**
 * Interface for handling incoming network packets from game clients.
 *
 * Implementations of this interface are responsible for processing specific
 * packet types received from clients and executing the corresponding game logic.
 */
interface PacketHandler {
    /**
     * Processes an incoming packet from a client.
     *
     * @param slea The packet data accessor providing methods to read the packet contents
     * @param c The client that sent the packet
     */
    fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client)

    /**
     * Validates whether the client is in a valid state to process this packet.
     *
     * @param c The client to validate
     * @return `true` if the client state is valid for handling this packet, `false` otherwise
     */
    fun validateState(c: Client): Boolean
}