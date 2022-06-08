package tools

import java.io.ByteArrayOutputStream
import kotlin.experimental.and

class HexTool {
    companion object {
        private val HEX = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

        fun toString(byteValue: Byte): String {
            val tmp: Int = byteValue.toInt() shl 8
            val ret = charArrayOf(
                HEX[tmp shr 12 and 0x0F],
                HEX[tmp shr 8 and 0x0F]
            )
            return ret.concatToString()
        }

        fun toString(bytes: ByteArray): String {
            val hexed = buildString {
                for (i in bytes.indices) {
                    append(toString(bytes[i]))
                    append(' ')
                }
            }
            return hexed.substring(0, hexed.length - 1)
        }

        fun getByteArrayFromHexString(hex: String): ByteArray {
            val bas = ByteArrayOutputStream()
            var nextI = 0
            var nextB = 0
            var highOc = true
            run outer@ {
                while (true) {
                    var number = -1
                    while (number == -1) {
                        if (nextI == hex.length) {
                            return@outer
                        }
                        val chr = hex[nextI]
                        number = when (chr) {
                            in '0'..'9' -> chr - '0'
                            in 'a'..'f' -> chr - 'a' + 10
                            in 'A'..'F' -> chr - 'A' + 10
                            else -> -1
                        }
                        nextI++
                    }
                    if (highOc) {
                        nextB = number shl 4
                        highOc = false
                    } else {
                        nextB = nextB or number
                        highOc = true
                        bas.write(nextB)
                    }
                }
            }
            return bas.toByteArray()
        }

        /**
         * Turns an array of bytes into a ASCII string. Any non-printable characters
         * are replaced by a period (<code>.</code>)
         *
         * @param bytes The bytes to convert.
         * @return The ASCII hexadecimal representation of <code>bytes</code>
         */
        fun toStringFromASCII(bytes: ByteArray): String {
            val ret = CharArray(bytes.size)
            for (x in bytes.indices) {
                if (bytes[x] in 0..31) {
                    ret[x] = '.'
                } else {
                    val chr: Int = bytes[x].toInt() and 0xFF
                    ret[x] = chr.toChar()
                }
            }
            return ret.concatToString()
        }
    }
}