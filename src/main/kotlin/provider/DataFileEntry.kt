package provider

/**
 * Represents a file-backed data entry within a hierarchical structure, providing metadata about its location and integrity.
 *
 * Extends [DataEntry] to inherit naming, size, checksum, and offset information,
 * and serves as a contract for entities that reference raw binary data stored in a file.
 */
interface DataFileEntry : DataEntry {
    /**
     * The byte offset within the data file where this entry begins.
     */
    override var offset: Int
}