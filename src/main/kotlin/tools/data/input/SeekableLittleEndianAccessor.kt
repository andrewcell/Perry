package tools.data.input

interface SeekableLittleEndianAccessor : LittleEndianAccessor {
    fun seek(offset: Long)
    val position: Long
}