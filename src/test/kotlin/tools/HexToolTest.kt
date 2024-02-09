package tools

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HexToolTest {
    @Test
    fun byteToHexStringConversion() {
        val byteValue: Byte = 0x1A
        val hexString = HexTool.toString(byteValue)
        assertEquals("1A", hexString)
    }

    @Test
    fun byteArrayToHexStringConversion() {
        val bytes = byteArrayOf(0x1A, 0x2B, 0x3C, 0x4D)
        val hexString = HexTool.toString(bytes)
        assertEquals("1A 2B 3C 4D", hexString)
    }

    @Test
    fun hexStringToByteArrayConversion() {
        val hexString = "1A 2B 3C 4D"
        val bytes = HexTool.getByteArrayFromHexString(hexString)
        assertArrayEquals(byteArrayOf(0x1A, 0x2B, 0x3C, 0x4D), bytes)
    }

    @Test
    fun byteArrayToAsciiStringConversion() {
        val bytes = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F)
        val asciiString = HexTool.toStringFromASCII(bytes)
        assertEquals("Hello", asciiString)
    }

    @Test
    fun byteArrayToAsciiStringConversionWithNonPrintableCharacters() {
        val bytes = byteArrayOf(0x01, 0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x1F)
        val asciiString = HexTool.toStringFromASCII(bytes)
        assertEquals(".Hello.", asciiString)
    }
}