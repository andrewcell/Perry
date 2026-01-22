package tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KMSEncryptionTest {

    @Test
    void encryptShouldReturnCorrectlyEncryptedData() {
        byte[] iv = new byte[]{0x12, 0x34, 0x56, 0x78};
        short gameVersion = 0x1234;
        KMSEncryption encryption = new KMSEncryption(iv, gameVersion);
        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04};

        byte[] encryptedData = encryption.encrypt(data);

        assertNotNull(encryptedData);
        assertNotEquals(new String(data), new String(encryptedData));
    }

    @Test
    void decryptShouldReturnOriginalData() {
        byte[] iv = new byte[]{0x12, 0x34, 0x56, 0x78};
        short gameVersion = 0x1234;
        KMSEncryption encryption = new KMSEncryption(iv, gameVersion);
        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04};

        byte[] encryptedData = encryption.encrypt(data);
        byte[] decryptedData = encryption.decrypt(encryptedData);

        assertArrayEquals(data, decryptedData);
    }

    @Test
    void getPacketHeaderShouldReturnValidHeader() {
        byte[] iv = new byte[]{0x12, 0x34, 0x56, 0x78};
        short gameVersion = 0x1234;
        KMSEncryption encryption = new KMSEncryption(iv, gameVersion);

        byte[] header = encryption.getPacketHeader(128);

        assertNotNull(header);
        assertEquals(4, header.length);
    }

    @Test
    void checkPacketShouldReturnTrueForValidPacket() {
        byte[] iv = new byte[]{0x12, 0x34, 0x56, 0x78};
        short gameVersion = 0x1234;
        KMSEncryption encryption = new KMSEncryption(iv, gameVersion);
        byte[] packet = new byte[]{(byte) (iv[2] ^ (gameVersion >> 8)), (byte) (iv[3] ^ gameVersion)};

        assertTrue(encryption.checkPacket(packet));
    }

    @Test
    void checkPacketShouldReturnFalseForInvalidPacket() {
        byte[] iv = new byte[]{0x12, 0x34, 0x56, 0x78};
        short gameVersion = 0x1234;
        KMSEncryption encryption = new KMSEncryption(iv, gameVersion);
        byte[] packet = new byte[]{0x00, 0x00};

        assertFalse(encryption.checkPacket(packet));
    }

    @Test
    void getPacketLengthShouldReturnCorrectLength() {
        int packetHeader = 0x12345678;

        int length = KMSEncryption.getPacketLength(packetHeader);

        assertEquals(0x5678, length);
    }

    @Test
    void getNewIvShouldGenerateDifferentIv() {
        byte[] oldIv = new byte[]{0x12, 0x34, 0x56, 0x78};

        byte[] newIv = KMSEncryption.getNewIv(oldIv);

        assertNotNull(newIv);
        assertEquals(4, newIv.length);
        assertNotEquals(new String(oldIv), new String(newIv));
    }

    @Test
    void getIv() {
    }

    @Test
    void encrypt() {
    }

    @Test
    void decrypt() {
    }

    @Test
    void getPacketHeader() {
    }

    @Test
    void getPacketLength() {
    }

    @Test
    void checkPacket() {
    }

    @Test
    void testCheckPacket() {
    }

    @Test
    void getNewIv() {
    }

    @Test
    void testToString() {
    }

    @Test
    void generateIv() {
    }
}