package provider

interface DataDirectoryEntry : DataEntry {
    val subDirectories: List<DataDirectoryEntry>
    val files: List<DataFileEntry>
    fun getEntry(name: String): DataEntry?
}