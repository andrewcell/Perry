package provider

/**
 * Represents a directory-like node in a hierarchical data structure, containing nested directories and files.
 *
 * Extends [DataEntry] to provide identification and metadata (size, checksum, offset), while adding
 * structural capabilities for managing child entries.
 */
interface DataDirectoryEntry : DataEntry {
    /**
     * The list of child directory entries contained within this directory.
     *
     * Each element represents a subdirectory that can be traversed, enabling hierarchical navigation
     * through the data structure. The order of entries is determined by the underlying data source.
     */
    val subDirectories: List<DataDirectoryEntry>
    /**
     * The list of files contained within this directory entry.
     *
     * Each element represents a file with metadata such as its location and size,
     * and provides access to the raw data through its [DataFileEntry.offset].
     */
    val files: List<DataFileEntry>
    /**
     * Retrieves the entry with the specified [name] from this directory.
     *
     * @param name The name of the entry to retrieve.
     * @return The [DataEntry] with the given name, or `null` if not found.
     */
    fun getEntry(name: String): DataEntry?
}