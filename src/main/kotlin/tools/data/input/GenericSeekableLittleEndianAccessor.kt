package tools.data.input

import mu.KLoggable

/**
 * Provides an abstract accessor to a generic Little Endian byte stream. This accessor is seekable.
 */
class GenericSeekableLittleEndianAccessor(override val bs: SeekableInputStreamByteStream) : GenericLittleEndianAccessor(bs), SeekableLittleEndianAccessor, KLoggable {
    override val logger = logger()

    /**
     * Seek the pointer to <code>offset</code>
     *
     * @param offset The offset to seek to.
     */
    override fun seek(offset: Long) {
        try {
            bs.seek(offset)
        } catch (e: Exception) {
            logger.error(e) { "Seek error." }
        }
    }

    override val position: Long
        get() = bs.position.toLong()

    /**
     * Skip <code>num</code> number of bytes in the stream.
     *
     * @param num The number of bytes to skip.
     */
    override fun skip(num: Int) {
        seek(position + num)
    }

}