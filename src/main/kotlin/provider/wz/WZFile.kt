package provider.wz

import mu.KLogging
import provider.Data
import provider.DataDirectoryEntry
import provider.DataProvider
import tools.data.input.*
import java.io.*

class WZFile(private val wzFile: File, private val provideImages: Boolean) : DataProvider {
    private val lea = GenericLittleEndianAccessor(InputStreamByteStream(BufferedInputStream(FileInputStream(wzFile))))
    val slea: SeekableLittleEndianAccessor
    override val root: WZDirectoryEntry
    private var cOffset = -1
    private var headerSize = -1

    init {
        val raf = RandomAccessFile(wzFile, "r")
        slea = GenericSeekableLittleEndianAccessor(RandomAccessByteStream(raf))
        root = WZDirectoryEntry(wzFile.name, 0, 0, null)
        load()
    }

    fun load() {
        lea.readASCIIString(4)
        lea.readInt()
        lea.readInt()
        headerSize = lea.readInt()
        lea.readNullTerminatedASCIIString()
        lea.readShort()
        parseDirectory(root)
        cOffset = lea.bytesRead.toInt()
        getOffsets(root)
    }

    private fun getOffsets(dir: DataDirectoryEntry) {
        dir.files.forEach {
            it.offset = cOffset
            cOffset += it.size
        }
        dir.subDirectories.forEach { getOffsets(it) }
    }

    private fun parseDirectory(dir: WZDirectoryEntry) {
        val entries = WZTool.readValue(lea)
        for (i in 0 until entries) {
            val marker = lea.readByte()
            var name: String
            var size: Int
            var checksum: Int
            when (marker.toInt()) {
                0x02 -> {
                    name = WZTool.readDecodedStringAtOffsetAndReset(slea, lea.readInt() + headerSize + 1)
                    size = WZTool.readValue(lea)
                    checksum = WZTool.readValue(lea)
                    lea.readInt()
                    dir.addFile(WZFileEntry(name, size, checksum, dir))
                }
                0x03, 0x04 -> {
                    name = WZTool.readDecodedString(lea)
                    size = WZTool.readValue(lea)
                    checksum = WZTool.readValue(lea)
                    lea.readInt() //dummy int
                    if (marker.toInt() == 3) {
                        dir.addDirectory(WZDirectoryEntry(name, size, checksum, dir))
                    } else {
                        dir.addFile(WZFileEntry(name, size, checksum, dir))
                    }
                }
            }
        }
        dir.subDirectories.forEach { parseDirectory(it as WZDirectoryEntry) }
    }

    private fun getImgFile(path: String): WZIMGFile? {
        val segments = path.split("/")
        var dir: WZDirectoryEntry? = root
        for (x in 0 until segments.size - 1) {
            dir = dir?.getEntry(segments[x]) as? WZDirectoryEntry
            if (dir == null) {
                return null
            }
        }
        val entry = dir?.getEntry(segments[segments.size - 1]) as? WZFileEntry ?: return null
        val fullPath = wzFile.name.substring(0, wzFile.name.length - 3).lowercase() + "/$path"
        return WZIMGFile(wzFile, entry, provideImages, ListWZFile.isModernImgFile(fullPath))
    }

    @Synchronized
    override fun getData(path: String): Data? {
        try {
            val imgFile = getImgFile(path) ?: return null
            return imgFile.root
        } catch (e: IOException) {
            logger.error(e) { "Error cause when get data from WZ file." }
        }
        return null
    }

    companion object : KLogging() {
        init {
            ListWZFile.init()
        }
    }
}