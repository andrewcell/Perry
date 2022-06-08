package provider.wz

import mu.KLoggable
import provider.Data
import provider.DataProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

class XMLWZFile(private val rootFile: File) : DataProvider, KLoggable {
    override val logger = logger()
    override val root = WZDirectoryEntry(rootFile.name, 0, 0, null)

    init {
        fillDataEntities(rootFile, root)
    }

    private fun fillDataEntities(lRoot: File, wzDir: WZDirectoryEntry) {
        lRoot.listFiles()?.forEach {
            val fileName = it.name
            if (it.isDirectory && !fileName.endsWith(".img")) {
                val newDir = WZDirectoryEntry(fileName, 0, 0, wzDir)
                wzDir.addDirectory(newDir)
                fillDataEntities(it, newDir)
            } else {
                wzDir.addFile(WZFileEntry(fileName.substring(0, fileName.length - 4), 0, 0, wzDir))
            }
        }
    }

    override fun getData(path: String): Data? {
        val dataFile = File(rootFile, "$path.xml")
        val imageDataDir = File(rootFile, path)
        if (!dataFile.exists()) return null
        try {
            val fis = FileInputStream(dataFile)
            val data =  XMLDomData(fis, imageDataDir.parentFile)
            fis.close()
            return data
        } catch (e: FileNotFoundException) {
            logger.error(e) { "Data file does not exist in ${rootFile.absolutePath}. Path: $path" }
        }
        return null
    }
}