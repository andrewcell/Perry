package tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KMSEncryptionTest {

    private static final byte[] IV = {0x12, 0x34, 0x56, 0x78};
    private static final short GAME_VERSION = 0x1234;

    // ─── getIv ───────────────────────────────────────────────────────────────

    @Test
    void getIv() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        assertArrayEquals(IV, enc.getIv());
    }

    // ─── encrypt ─────────────────────────────────────────────────────────────

    @Test
    void encrypt() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        byte[] encrypted = enc.encrypt(data.clone());
        assertNotNull(encrypted);
        assertEquals(data.length, encrypted.length);
        assertFalse(java.util.Arrays.equals(data, encrypted),
                "Encrypted output should differ from plaintext");
    }

    @Test
    void encryptShouldReturnCorrectlyEncryptedData() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        byte[] encrypted = enc.encrypt(data.clone());
        assertFalse(java.util.Arrays.equals(data, encrypted));
    }

    // ─── decrypt ─────────────────────────────────────────────────────────────

    // encrypt() calls updateIv() after finishing, so the IV changes.
    // decrypt() must start with the same IV as encrypt() did —
    // use two separate instances initialised with the same IV.
    @Test
    void decrypt() {
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        KMSEncryption encEnc = new KMSEncryption(IV.clone(), GAME_VERSION);
        KMSEncryption encDec = new KMSEncryption(IV.clone(), GAME_VERSION);
        byte[] encrypted = encEnc.encrypt(data.clone());
        byte[] decrypted = encDec.decrypt(encrypted);
        assertArrayEquals(data, decrypted);
    }

    @Test
    void decryptShouldReturnOriginalData() {
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        KMSEncryption encEnc = new KMSEncryption(IV.clone(), GAME_VERSION);
        KMSEncryption encDec = new KMSEncryption(IV.clone(), GAME_VERSION);
        byte[] encrypted = encEnc.encrypt(data.clone());
        byte[] decrypted = encDec.decrypt(encrypted);
        assertArrayEquals(data, decrypted);
    }

    // ─── getPacketHeader ─────────────────────────────────────────────────────

    @Test
    void getPacketHeader() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        byte[] header = enc.getPacketHeader(256);
        assertNotNull(header);
        assertEquals(4, header.length);
    }

    @Test
    void getPacketHeaderShouldReturnValidHeader() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        byte[] header = enc.getPacketHeader(128);
        assertNotNull(header);
        assertEquals(4, header.length);
    }

    // ─── getPacketLength ─────────────────────────────────────────────────────

    @Test
    void getPacketLength() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        int originalLength = 100;
        byte[] header = enc.getPacketHeader(originalLength);
        int packetHeader = ((header[0] & 0xFF) << 24)
                         | ((header[1] & 0xFF) << 16)
                         | ((header[2] & 0xFF) << 8)
                         |  (header[3] & 0xFF);
        assertEquals(originalLength, KMSEncryption.getPacketLength(packetHeader));
    }

    @Test
    void getPacketLengthShouldReturnCorrectLength() {
        // Use a round-trip via getPacketHeader to get a valid packetHeader integer
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        int originalLength = 256;
        byte[] header = enc.getPacketHeader(originalLength);
        int packetHeader = ((header[0] & 0xFF) << 24)
                         | ((header[1] & 0xFF) << 16)
                         | ((header[2] & 0xFF) << 8)
                         |  (header[3] & 0xFF);
        assertEquals(originalLength, KMSEncryption.getPacketLength(packetHeader));
    }

    // ─── checkPacket(byte[]) ─────────────────────────────────────────────────

    @Test
    void checkPacket() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        // gameVersion is byte-swapped in the constructor: store the swapped value
        short swapped = (short) (((GAME_VERSION >> 8) & 0xFF) | ((GAME_VERSION << 8) & 0xFF00));
        byte[] validPacket = new byte[]{
            (byte) (IV[2] ^ ((swapped >> 8) & 0xFF)),
            (byte) (IV[3] ^ (swapped & 0xFF))
        };
        assertTrue(enc.checkPacket(validPacket));
    }

    @Test
    void checkPacketShouldReturnFalseForInvalidPacket() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        assertFalse(enc.checkPacket(new byte[]{0x00, 0x00}));
    }

    // ─── checkPacket(int) ────────────────────────────────────────────────────

    @Test
    void testCheckPacket() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        short swapped = (short) (((GAME_VERSION >> 8) & 0xFF) | ((GAME_VERSION << 8) & 0xFF00));
        byte b0 = (byte) (IV[2] ^ ((swapped >> 8) & 0xFF));
        byte b1 = (byte) (IV[3] ^ (swapped & 0xFF));
        int packetHeader = ((b0 & 0xFF) << 24) | ((b1 & 0xFF) << 16);
        assertTrue(enc.checkPacket(packetHeader));
    }

    // ─── getNewIv ────────────────────────────────────────────────────────────

    @Test
    void getNewIv() {
        byte[] oldIv = {0x12, 0x34, 0x56, 0x78};
        byte[] newIv = KMSEncryption.getNewIv(oldIv.clone());
        assertNotNull(newIv);
        assertEquals(4, newIv.length);
        assertFalse(java.util.Arrays.equals(oldIv, newIv));
    }

    @Test
    void getNewIvShouldGenerateDifferentIv() {
        byte[] oldIv = {0x12, 0x34, 0x56, 0x78};
        byte[] newIv = KMSEncryption.getNewIv(oldIv.clone());
        assertFalse(java.util.Arrays.equals(oldIv, newIv));
    }

    @Test
    void getNewIvIsDeterministic() {
        byte[] iv = {0x12, 0x34, 0x56, 0x78};
        assertArrayEquals(
            KMSEncryption.getNewIv(iv.clone()),
            KMSEncryption.getNewIv(iv.clone())
        );
    }

    // ─── generateIv ──────────────────────────────────────────────────────────

    @Test
    void generateIv() {
        byte[] in1 = {0x12, 0x34, 0x56, 0x78};
        byte[] before = in1.clone();
        byte[] result = KMSEncryption.generateIv(0xAB, in1);
        assertSame(result, in1);
        assertFalse(java.util.Arrays.equals(before, result));
    }

    @Test
    void generateIvIsDeterministic() {
        byte[] in1 = {0x12, 0x34, 0x56, 0x78};
        byte[] in2 = {0x12, 0x34, 0x56, 0x78};
        assertArrayEquals(
            KMSEncryption.generateIv(0xAB, in1),
            KMSEncryption.generateIv(0xAB, in2)
        );
    }

    // ─── toString ────────────────────────────────────────────────────────────

    @Test
    void testToString() {
        KMSEncryption enc = new KMSEncryption(IV.clone(), GAME_VERSION);
        String str = enc.toString();
        assertNotNull(str);
        assertTrue(str.contains("IV:"), "toString should contain 'IV:' but was: " + str);
    }
}