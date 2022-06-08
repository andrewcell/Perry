package tools.data.input

import tools.HexTool
import kotlin.experimental.and

class ByteArrayByteStream(val array: ByteArray) : SeekableInputStreamByteStream {
    override var position = 0
    override var bytesRead = 0L

    override fun seek(offset: Long) {
        position = offset.toInt()
    }

    override fun readByte(): Int {
        bytesRead++
        return array[position++].toInt() and 0xFF
    }

    override fun toString(): String {
        var str = "END OF STRING"
        if (array.size - position > 0) {
            val now = ByteArray(array.size - position)
            System.arraycopy(array, position, now, 0, array.size - position)
            str = HexTool.toString(now)
        }
        return "ALL: ${HexTool.toString(array)}\nNow: $str"
    }

    override fun available() = (array.size - position).toLong()
}