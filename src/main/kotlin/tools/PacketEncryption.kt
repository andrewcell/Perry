package tools

/**
 * PacketEncryption is an interface that provides methods for packet encryption and decryption.
 * It is used in the context of network communication where data packets need to be encrypted before transmission
 * and decrypted upon receipt for security purposes.
 */
interface PacketEncryption {
    /**
     * Checks if the packet header is valid.
     * This is typically used to verify the integrity of the packet before processing.
     *
     * @param header The packet header to check.
     * @return True if the packet header is valid, false otherwise.
     */
    fun checkPacket(header: Int): Boolean

    /**
     * Encrypts the given data.
     * This method is used to secure the data before transmission.
     *
     * @param data The data to encrypt.
     * @return The encrypted data.
     */
    fun encrypt(data: ByteArray): ByteArray

    /**
     * Decrypts the given data.
     * This method is used to retrieve the original data after receipt.
     *
     * @param data The data to decrypt.
     * @return The decrypted data.
     */
    fun decrypt(data: ByteArray): ByteArray

    /**
     * Retrieves the packet header for a packet of the given length.
     * This is typically used to prepare the packet for transmission by attaching the appropriate header.
     *
     * @param length The length of the packet.
     * @return The packet header.
     */
    fun getPacketHeader(length: Int): ByteArray
}