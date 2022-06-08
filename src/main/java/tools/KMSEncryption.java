package tools;

/**
 * Provides a class for en/decrypting Korean Client Packets.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 320
 */
public class KMSEncryption implements PacketEncryption {

    private byte iv[];
    private short gameVersion;
    private static final byte[] funnyBytes = new byte[]{(byte) 0xEC, (byte) 0x3F, (byte) 0x77, (byte) 0xA4, (byte) 0x45, (byte) 0xD0, (byte) 0x71, (byte) 0xBF, (byte) 0xB7, (byte) 0x98, (byte) 0x20, (byte) 0xFC,
        (byte) 0x4B, (byte) 0xE9, (byte) 0xB3, (byte) 0xE1, (byte) 0x5C, (byte) 0x22, (byte) 0xF7, (byte) 0x0C, (byte) 0x44, (byte) 0x1B, (byte) 0x81, (byte) 0xBD, (byte) 0x63, (byte) 0x8D, (byte) 0xD4, (byte) 0xC3,
        (byte) 0xF2, (byte) 0x10, (byte) 0x19, (byte) 0xE0, (byte) 0xFB, (byte) 0xA1, (byte) 0x6E, (byte) 0x66, (byte) 0xEA, (byte) 0xAE, (byte) 0xD6, (byte) 0xCE, (byte) 0x06, (byte) 0x18, (byte) 0x4E, (byte) 0xEB,
        (byte) 0x78, (byte) 0x95, (byte) 0xDB, (byte) 0xBA, (byte) 0xB6, (byte) 0x42, (byte) 0x7A, (byte) 0x2A, (byte) 0x83, (byte) 0x0B, (byte) 0x54, (byte) 0x67, (byte) 0x6D, (byte) 0xE8, (byte) 0x65, (byte) 0xE7,
        (byte) 0x2F, (byte) 0x07, (byte) 0xF3, (byte) 0xAA, (byte) 0x27, (byte) 0x7B, (byte) 0x85, (byte) 0xB0, (byte) 0x26, (byte) 0xFD, (byte) 0x8B, (byte) 0xA9, (byte) 0xFA, (byte) 0xBE, (byte) 0xA8, (byte) 0xD7,
        (byte) 0xCB, (byte) 0xCC, (byte) 0x92, (byte) 0xDA, (byte) 0xF9, (byte) 0x93, (byte) 0x60, (byte) 0x2D, (byte) 0xDD, (byte) 0xD2, (byte) 0xA2, (byte) 0x9B, (byte) 0x39, (byte) 0x5F, (byte) 0x82, (byte) 0x21,
        (byte) 0x4C, (byte) 0x69, (byte) 0xF8, (byte) 0x31, (byte) 0x87, (byte) 0xEE, (byte) 0x8E, (byte) 0xAD, (byte) 0x8C, (byte) 0x6A, (byte) 0xBC, (byte) 0xB5, (byte) 0x6B, (byte) 0x59, (byte) 0x13, (byte) 0xF1,
        (byte) 0x04, (byte) 0x00, (byte) 0xF6, (byte) 0x5A, (byte) 0x35, (byte) 0x79, (byte) 0x48, (byte) 0x8F, (byte) 0x15, (byte) 0xCD, (byte) 0x97, (byte) 0x57, (byte) 0x12, (byte) 0x3E, (byte) 0x37, (byte) 0xFF,
        (byte) 0x9D, (byte) 0x4F, (byte) 0x51, (byte) 0xF5, (byte) 0xA3, (byte) 0x70, (byte) 0xBB, (byte) 0x14, (byte) 0x75, (byte) 0xC2, (byte) 0xB8, (byte) 0x72, (byte) 0xC0, (byte) 0xED, (byte) 0x7D, (byte) 0x68,
        (byte) 0xC9, (byte) 0x2E, (byte) 0x0D, (byte) 0x62, (byte) 0x46, (byte) 0x17, (byte) 0x11, (byte) 0x4D, (byte) 0x6C, (byte) 0xC4, (byte) 0x7E, (byte) 0x53, (byte) 0xC1, (byte) 0x25, (byte) 0xC7, (byte) 0x9A,
        (byte) 0x1C, (byte) 0x88, (byte) 0x58, (byte) 0x2C, (byte) 0x89, (byte) 0xDC, (byte) 0x02, (byte) 0x64, (byte) 0x40, (byte) 0x01, (byte) 0x5D, (byte) 0x38, (byte) 0xA5, (byte) 0xE2, (byte) 0xAF, (byte) 0x55,
        (byte) 0xD5, (byte) 0xEF, (byte) 0x1A, (byte) 0x7C, (byte) 0xA7, (byte) 0x5B, (byte) 0xA6, (byte) 0x6F, (byte) 0x86, (byte) 0x9F, (byte) 0x73, (byte) 0xE6, (byte) 0x0A, (byte) 0xDE, (byte) 0x2B, (byte) 0x99,
        (byte) 0x4A, (byte) 0x47, (byte) 0x9C, (byte) 0xDF, (byte) 0x09, (byte) 0x76, (byte) 0x9E, (byte) 0x30, (byte) 0x0E, (byte) 0xE4, (byte) 0xB2, (byte) 0x94, (byte) 0xA0, (byte) 0x3B, (byte) 0x34, (byte) 0x1D,
        (byte) 0x28, (byte) 0x0F, (byte) 0x36, (byte) 0xE3, (byte) 0x23, (byte) 0xB4, (byte) 0x03, (byte) 0xD8, (byte) 0x90, (byte) 0xC8, (byte) 0x3C, (byte) 0xFE, (byte) 0x5E, (byte) 0x32, (byte) 0x24, (byte) 0x50,
        (byte) 0x1F, (byte) 0x3A, (byte) 0x43, (byte) 0x8A, (byte) 0x96, (byte) 0x41, (byte) 0x74, (byte) 0xAC, (byte) 0x52, (byte) 0x33, (byte) 0xF0, (byte) 0xD9, (byte) 0x29, (byte) 0x80, (byte) 0xB1, (byte) 0x16,
        (byte) 0xD3, (byte) 0xAB, (byte) 0x91, (byte) 0xB9, (byte) 0x84, (byte) 0x7F, (byte) 0x61, (byte) 0x1E, (byte) 0xCF, (byte) 0xC5, (byte) 0xD1, (byte) 0x56, (byte) 0x3D, (byte) 0xCA, (byte) 0xF4, (byte) 0x05,
        (byte) 0xC6, (byte) 0xE5, (byte) 0x08, (byte) 0x49};

    /**
     * Class constructor - Creates an instance of the Client encryption
     * cipher.
     *
     * @param gameVersion The 256 bit AES key to use.
     * @param iv The 4-byte IV to use.
     */
    public KMSEncryption(byte iv[], short gameVersion) {
        this.setIv(iv);
        this.gameVersion = (short) (((gameVersion >> 8) & 0xFF) | ((gameVersion << 8) & 0xFF00));
    }

    /**
     * Sets the IV of this instance.
     *
     * @param iv The new IV.
     */
    private void setIv(byte[] iv) {
        this.iv = iv;
    }

    /**
     * For debugging/testing purposes only.
     *
     * @return The IV.
     */
    public byte[] getIv() {
        return this.iv;
    }

    @Override public byte[] encrypt(final byte[] data) {
        byte[] datacpy = new byte[data.length];
        System.arraycopy(data, 0, datacpy, 0, data.length);
        byte[] in = new byte[this.iv.length];
        System.arraycopy(this.iv, 0, in, 0, in.length);

        for (int x = 0; x < datacpy.length; ++x) {
            int byt = (int) datacpy[x] & 0xFF;
            byt = ((byt << 4) & 0xF0) | ((byt >> 4) & 0x0F);
            byt = ((byt << 1) & 0xFE) ^ ((((byt << 1) & 0xFE) ^ ((byt >> 1) & 0x7F)) & 0x55);
            byt ^= funnyBytes[(int) in[0] & 0xFF];
            in = generateIv((int) datacpy[x] & 0xFF, in);
            datacpy[x] = (byte) byt;
        }

        updateIv();
        return datacpy;
    }

    @Override public byte[] decrypt(byte[] data) {
        byte[] in = new byte[this.iv.length];
        System.arraycopy(this.iv, 0, in, 0, in.length);

        for (int x = 0; x < data.length; ++x) {
            int byt = (int) data[x] & 0xFF;
            byt ^= funnyBytes[(int) in[0] & 0xFF];
            byt = ((byt << 1) & 0xFE) ^ ((((byt << 1) & 0xFE) ^ ((byt >> 1) & 0x7F)) & 0x55);
            byt = ((byt << 4) & 0xF0) | ((byt >> 4) & 0x0F);
            in = generateIv(byt, in);
            data[x] = (byte) byt;
        }

        updateIv();
        return data;
    }

    /**
     * Generates a new IV.
     */
    private void updateIv() {
        this.iv = getNewIv(this.iv);
    }

    /**
     * Generates a packet header for a packet that is
     * <code>length</code> long.
     *
     * @param length How long the packet that this header is for is.
     * @return The header.
     */
    @Override public byte[] getPacketHeader(int length) {
        int iiv = (((iv[3]) & 0xFF) | ((iv[2] << 8) & 0xFF00)) ^ gameVersion;
        int mlength = (((length << 8) & 0xFF00) | (length >>> 8)) ^ iiv;

        return new byte[]{(byte) ((iiv >>> 8) & 0xFF), (byte) (iiv & 0xFF), (byte) ((mlength >>> 8) & 0xFF), (byte) (mlength & 0xFF)};
    }

    /**
     * Gets the packet length from a header.
     *
     * @param packetHeader The header as an integer.
     * @return The length of the packet.
     */
    public static int getPacketLength(int packetHeader) {
        int packetLength = ((packetHeader >>> 16) ^ (packetHeader & 0xFFFF));
        packetLength = ((packetLength << 8) & 0xFF00) | ((packetLength >>> 8) & 0xFF); // fix endianness
        return packetLength;
    }

    /**
     * Check the packet to make sure it has a header.
     *
     * @param packet The packet to check.
     * @return <code>True</code> if the packet has a correct header,
     * <code>false</code> otherwise.
     */
    public boolean checkPacket(byte[] packet) {
        return ((((packet[0] ^ iv[2]) & 0xFF) == ((gameVersion >> 8) & 0xFF)) && (((packet[1] ^ iv[3]) & 0xFF) == (gameVersion & 0xFF)));
    }

    /**
     * Check the header for validity.
     *
     * @param packetHeader The packet header to check.
     * @return <code>True</code> if the header is correct, <code>false</code>
     * otherwise.
     */
    public boolean checkPacket(int packetHeader) {
        return checkPacket(new byte[]{(byte) ((packetHeader >> 24) & 0xFF), (byte) ((packetHeader >> 16) & 0xFF)});
    }

    /**
     * Gets a new IV from
     * <code>oldIv</code>
     *
     * @param oldIv The old IV to get a new IV from.
     * @return The new IV.
     */
    public static byte[] getNewIv(byte oldIv[]) {
        byte[] in = {(byte) 0xf2, (byte) 0x53, (byte) 0x50, (byte) 0xc6}; // magic
        // ;)
        for (int x = 0; x < 4; x++) {
            generateIv((int) oldIv[x] & 0xFF, in);
            //System.out.println(HexTool.toString(in));
        }
        return in;
    }

    /**
     * Returns the IV of this instance as a string.
     */
    @Override
    public String toString() {
        return "IV: " + HexTool.Companion.toString(this.iv);
    }

    /**
     * Does funny stuff.
     * <code>this.OldIV</code> must not equal
     * <code>in</code> Modifies
     * <code>in</code> and returns it for convenience.
     *
     * @param inputByte The byte to apply the funny stuff to.
     * @param in Something needed for all this to occur.
     * @return The modified version of <code>in</code>.
     */
    public static byte[] generateIv(int inputByte, byte[] in) {

        in[0] += funnyBytes[(int) in[1] & 0xFF] - (inputByte & 0xFF);
        in[1] -= in[2] ^ funnyBytes[(int) inputByte & 0xFF];
        in[2] ^= (inputByte & 0xFF) + funnyBytes[(int) in[3] & 0xFF];
        in[3] = (byte) ((in[3] - in[0] + funnyBytes[(int) inputByte & 0xFF]) & 0xFF);

        int ret_value = ((int) in[0]) & 0xFF;
        ret_value |= (in[1] << 8) & 0xFF00;
        ret_value |= (in[2] << 16) & 0xFF0000;
        ret_value |= (in[3] << 24) & 0xFF000000;

        // rol ret_value, 3
        ret_value = ((ret_value << 3) & 0xFFFFFFF8) | ((ret_value >> 29) & 7);

        in[0] = (byte) (ret_value & 0xFF);
        in[1] = (byte) ((ret_value >> 8) & 0xFF);
        in[2] = (byte) ((ret_value >> 16) & 0xFF);
        in[3] = (byte) ((ret_value >> 24) & 0xFF);
        return in;
    }
}
