package tools

import kotlin.experimental.and
import kotlin.experimental.xor


/**
 * Provides a class for en/decrypting Korean Client Packets.
 *
 * Ported from the original Java implementation.
 */
class KMSEncryption2(
    private var iv: ByteArray,
    gameVersion: Short
) : PacketEncryption {

    private val gameVersion: Short =
        (((gameVersion.toInt() shr 8) and 0xFF) or ((gameVersion.toInt() shl 8) and 0xFF00)).toShort()

    override fun encrypt(data: ByteArray): ByteArray {
        val datacpy = data.copyOf()
        val inArr = iv.copyOf()

        for (x in datacpy.indices) {
            var byt = datacpy[x].toInt() and 0xFF
            byt = ((byt shl 4) and 0xF0) or ((byt shr 4) and 0x0F)
            byt = ((byt shl 1) and 0xFE) xor ((((byt shl 1) and 0xFE) xor ((byt shr 1) and 0x7F)) and 0x55)
            byt = byt xor ivKeys[inArr[0].toInt() and 0xFF].toInt()
            shuffleIv(datacpy[x]and 0xFF.toByte(), inArr)
            datacpy[x] = byt.toByte()
        }

        updateIv()
        return datacpy
    }

    override fun decrypt(data: ByteArray): ByteArray {
        val inArr = iv.copyOf()

        for (x in data.indices) {
            var byt = data[x].toInt() and 0xFF
            byt = byt xor ivKeys[inArr[0].toInt() and 0xFF].toInt()
            byt = ((byt shl 1) and 0xFE) xor ((((byt shl 1) and 0xFE) xor ((byt shr 1) and 0x7F)) and 0x55)
            byt = ((byt shl 4) and 0xF0) or ((byt shr 4) and 0x0F)
            shuffleIv(byt.toByte(), inArr)
            data[x] = byt.toByte()
        }

        updateIv()
        return data
    }

    private fun updateIv() {
        iv = getNewIv(iv)
    }

    override fun getPacketHeader(length: Int): ByteArray {
        val iiv = ((iv[3].toInt() and 0xFF) or ((iv[2].toInt() shl 8) and 0xFF00)) xor gameVersion.toInt()
        val mlength = (((length shl 8) and 0xFF00) or (length ushr 8)) xor iiv

        return byteArrayOf(
            ((iiv ushr 8) and 0xFF).toByte(),
            (iiv and 0xFF).toByte(),
            ((mlength ushr 8) and 0xFF).toByte(),
            (mlength and 0xFF).toByte()
        )
    }

    fun checkPacket(packet: ByteArray): Boolean {
        return (((packet[0] xor iv[2]) and 0xFF.toByte()) == ((gameVersion.toInt() shr 8) and 0xFF).toByte()
                && ((packet[1] xor iv[3]) and 0xFF.toByte()) == (gameVersion.toInt() and 0xFF).toByte())
    }

    override fun checkPacket(header: Int): Boolean {
        return checkPacket(
            byteArrayOf(
                ((header shr 24) and 0xFF).toByte(),
                ((header shr 16) and 0xFF).toByte()
            )
        )
    }

    override fun toString(): String {
        return "IV: ${HexTool.toString(iv)}"
    }

    companion object {
        val ivKeys = ("ec30760145d072bfb79820fc4be9b3ad5c22f70c421381bd638dd4c3f21019e0fba16e66e" +
                "aaed6ce06184eeb7895dbbab6427a2a830b54676de865e72f07f3aa277b85b026fdbba9fabea8d" +
                "7cbcc92daf993602dddd2a29b395f82214c69f83187ee8ead8c6abcb56b5913f10400f65a35794" +
                "88f15cd9757123e37ff9d4f51f5a370bb1475c2b872c0ed7d68c92e0d624617114d6cc47e53c12" +
                "5c79a1c88582c89dc026440015d38a5e2af55d5ef1a7ca75ba66f869f73e60ade2b994a479cdf0" +
                "9769e300ee4b294a03b341d280f36e323b403d890c83cfe5e3224501f3a438a964174ac5233f0d" +
                "92980b116d3ab91b9847f611ecfc5d1563dcaf405c6e50849").hexToByteArray()

        fun getNewIv(oldIv: ByteArray): ByteArray {
            val inArr = "f25350c6".hexToByteArray()
            repeat(4) { x ->
                shuffleIv(oldIv[x], inArr)
            }
            return inArr
        }

        // From AeosDev and some modified by tsun
        fun shuffleIv(inputByte: Byte, iv: ByteArray): ByteArray {
            // Step 1: mutate IV using ivkeys
            iv[0] = ((iv[0].toInt() and 0xFF) +
                    (ivKeys[iv[1].toInt() and 0xFF].toInt() and 0xFF) -
                    (inputByte.toInt() and 0xFF)).toByte()
            iv[1] = ((iv[1].toInt() and 0xFF) -
                    ((iv[2].toInt() and 0xFF) xor (ivKeys[inputByte.toInt() and 0xFF].toInt() and 0xFF))).toByte()
            iv[2] = ((iv[2].toInt() and 0xFF) xor
                    ((inputByte.toInt() and 0xFF) + (ivKeys[iv[3].toInt() and 0xFF].toInt() and 0xFF))).toByte()
            iv[3] = ((iv[3].toInt() and 0xFF) -
                    (iv[0].toInt() and 0xFF) +
                    (ivKeys[inputByte.toInt() and 0xFF].toInt() and 0xFF)).toByte()

            // Step 2: pack into 32-bit int
            var ret = (iv[0].toInt() and 0xFF)
            ret = ret or ((iv[1].toInt() shl 8) and 0xFF00)
            ret = ret or ((iv[2].toInt() shl 16) and 0xFF0000)
            ret = ret or ((iv[3].toInt() shl 24) and -0x1000000)

            // Step 3: rotate left by 3 bits
            ret = ((ret shl 3) and -0x8) or ((ret shr 29) and 7)

            // Step 4: unpack back into bytes
            iv[0] = (ret and 0xFF).toByte()
            iv[1] = ((ret shr 8) and 0xFF).toByte()
            iv[2] = ((ret shr 16) and 0xFF).toByte()
            iv[3] = ((ret shr 24) and 0xFF).toByte()
            return iv
        }

        fun getPacketLength(packetHeader: Int): Int {
            var packetLength = ((packetHeader ushr 16) xor (packetHeader and 0xFFFF))
            packetLength = ((packetLength shl 8) and 0xFF00) or ((packetLength ushr 8) and 0xFF)
            return packetLength
        }
    }
}