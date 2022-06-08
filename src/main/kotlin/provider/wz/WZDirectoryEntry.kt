package provider.wz

import provider.DataDirectoryEntry
import provider.DataEntity
import provider.DataEntry
import provider.DataFileEntry

class WZDirectoryEntry : WZEntry, DataDirectoryEntry {
    override val subDirectories = mutableListOf<DataDirectoryEntry>()
    override val files = mutableListOf<DataFileEntry>()
    val entries = mutableMapOf<String, DataEntry>()

    constructor() : super("", 0, 0, null)

    constructor(name: String, size: Int, checksum: Int, parent: DataEntity?) : super(name, size, checksum, parent)

    fun addDirectory(dir: DataDirectoryEntry) {
        subDirectories.add(dir)
        entries[dir.name] = dir
    }

    fun addFile(fileEntry: DataFileEntry) {
        files.add(fileEntry)
        entries[fileEntry.name] = fileEntry
    }

    override fun getEntry(name: String): DataEntry? = entries[name]
}