package provider

interface DataProvider {
    fun getData(path: String): Data?
    val root: DataDirectoryEntry
}