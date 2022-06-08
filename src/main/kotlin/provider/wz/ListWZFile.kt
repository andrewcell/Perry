package provider.wz

import provider.DataProviderFactory
import tools.data.input.GenericLittleEndianAccessor
import tools.data.input.InputStreamByteStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

class ListWZFile(val listWz: File) {
    val lea = GenericLittleEndianAccessor(InputStreamByteStream(BufferedInputStream(FileInputStream(listWz))))
    val entries: List<String>

    init {
        val list = mutableListOf<String>()
        while (lea.available() > 0) {
            val l = lea.readInt() * 2
            val chunk = ByteArray(l)
            for (i in chunk.indices) {
                chunk[i] = lea.readByte()
            }
            lea.readChar()
            val value = WZTool.readListString(chunk)
            list.add(value.toString())
        }
        entries = list
    }

    companion object {
        private var modernImgs: Collection<String> = HashSet()

        fun init() {
            val listWzStr = System.getProperty("listwz")
            if (listWzStr != null) {
                val listWz = ListWZFile(DataProviderFactory.fileInWzPath("List.wz"))
                modernImgs = HashSet<String>(listWz.entries)
            }
        }

        fun isModernImgFile(path: String) = modernImgs.contains(path)
    }
}