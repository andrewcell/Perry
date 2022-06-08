package provider.wz

import mu.KLogging
import tools.data.input.LittleEndianAccessor
import tools.data.input.SeekableLittleEndianAccessor
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

class WZTool {
    companion object : KLogging() {
        val encKey: ByteArray

        init {
            var iv = byteArrayOf(
                0x4d.toByte(), 0x23.toByte(), 0xc7.toByte(), 0x2b.toByte(), 0x4d.toByte(), 0x23.toByte(),
                0xc7.toByte(), 0x2b.toByte(), 0x4d.toByte(), 0x23.toByte(), 0xc7.toByte(), 0x2b.toByte(),
                0x4d.toByte(), 0x23.toByte(), 0xc7.toByte(), 0x2b.toByte()
            )
            val key = byteArrayOf(0x13.toByte(), 0x00, 0x00, 0x00, 0x08.toByte(),
                0x00, 0x00, 0x00, 0x06.toByte(), 0x00, 0x00, 0x00, 0xB4.toByte(), 0x00,
                0x00, 0x00, 0x1B.toByte(), 0x00, 0x00, 0x00, 0x0F.toByte(), 0x00, 0x00,
                0x00, 0x33.toByte(), 0x00, 0x00, 0x00, 0x52.toByte(), 0x00, 0x00, 0x00
            )
            encKey = ByteArray(0xFFFF)
            try {
                val sKeySpec = SecretKeySpec(key, "AES")
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.ENCRYPT_MODE, sKeySpec)
                for (i in 0 until 0xFFFF / 16) {
                    iv = cipher.doFinal(iv)
                    System.arraycopy(iv, 0, encKey, (i * 16), 16)
                }
                iv = cipher.doFinal(iv)
                System.arraycopy(iv, 0, encKey, 65520, 15)
            } catch (e: Exception) {
                logger.error(e) { "Failed to load WZ file loader." }
            }
        }

        fun readListString(str: ByteArray): ByteArray {
            for (i in str.indices) {
                str[i] = str[i] xor encKey[i]
            }
            return str
        }

        fun readDecodedString(llea: LittleEndianAccessor): String {
            val strLength: Int
            val b = llea.readByte()
            if (b.toInt() == 0x00) return ""
            return if (b >= 0) {
                strLength = if (b.toInt() == 0x7F)
                    llea.readInt()
                else b.toInt()
                if (strLength < 0) return ""
                val str = ByteArray(strLength * 2)
                for (i in 0 until strLength * 2) {
                    str[i] = llea.readByte()
                }
                decryptUnicodeStr(str)
            } else {
                strLength = if (b.toInt() == -128)
                    llea.readInt()
                else -b.toInt()
                if (strLength < 0) return ""
                val str = ByteArray(strLength)
                for (i in 0 until strLength) {
                    str[i] = llea.readByte()
                }
                decryptASCIIStr(str)
            }
        }

        fun decryptASCIIStr(str: ByteArray): String {
            var xorByte = 0xAA.toByte()
            for (i in str.indices) {
                str[i] = str[i] xor xorByte xor encKey[i]
                xorByte++
            }
            return str.decodeToString()
        }

        fun decryptUnicodeStr(str: ByteArray): String {
            var xorByte = 0xAAAA
            val charRet = CharArray(str.size / 2)
            for (i in str.indices) {
                str[i] = str[i] xor encKey[i]
            }
            for (i in 0 until str.size / 2) {
                val toXor: Char = (str[i].toInt() shl 8 or str[i + 1].toInt()).toChar()
                charRet[i] = (toXor.code xor xorByte).toChar()
                xorByte++
            }
            return charRet.concatToString()
        }

        fun readDecodedStringAtOffset(slea: SeekableLittleEndianAccessor, offset: Int): String {
            slea.seek(offset.toLong())
            return readDecodedString(slea)
        }

        fun readDecodedStringAtOffsetAndReset(slea: SeekableLittleEndianAccessor, offset: Int): String {
            val pos = slea.position
            slea.seek(offset.toLong())
            val ret = readDecodedString(slea)
            slea.seek(pos)
            return ret
        }

        fun readValue(lea: LittleEndianAccessor): Int {
            val b = lea.readByte().toInt()
            return if (b == -128) lea.readInt()
            else b
        }

        fun readFloatValue(lea: LittleEndianAccessor): Float {
            val b = lea.readByte().toInt()
            return if (b == -128) lea.readFloat()
            else 0f
        }
    }
}