package tools.data.input

import java.awt.Point

interface LittleEndianAccessor {
    fun readByte(): Byte
    fun readChar(): Char
    fun readShort(): Short
    fun readInt(): Int
    fun readPos(): Point
    fun readLong(): Long
    fun skip(num: Int)
    fun read(num: Int): ByteArray
    fun readFloat(): Float
    fun readDouble(): Double
    fun readASCIIString(n: Int): String
    fun readNullTerminatedASCIIString(): String
    fun readGameASCIIString(): String
    fun available(): Long
    val bytesRead: Long
}