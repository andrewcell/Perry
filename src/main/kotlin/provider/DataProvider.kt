package provider

/**
 * Provides access to hierarchical data structures, typically representing file systems or content archives such as WZ files.
 *
 * This interface abstracts the underlying storage mechanism and allows retrieval of data nodes through paths,
 * while also exposing the root directory entry for navigation and inspection.
 */
interface DataProvider {
    /**
     * Retrieves a [Data] node located at the specified hierarchical path relative to the current data provider's root.
     *
     * The method resolves the given path by traversing the tree structure starting from the [root] entry,
     * interpreting each segment of the slash-separated path as a child name. Returns `null` if any segment
     * cannot be resolved or if the final target does not exist.
     *
     * @param path A slash-separated string representing the hierarchical path to the desired data node,
     * such as `"node1/node2/leaf"`.
     * @return The [Data] instance at the specified path, or `null` if the path is invalid or the element
     * does not exist.
     */
    fun getData(path: String): Data?
    /**
     * The root directory entry of the hierarchical data structure.
     *
     * Represents the top-level node from which all nested directories and files can be accessed.
     * Provides initial access to the entire data tree through [DataDirectoryEntry.subDirectories] and
     * [DataDirectoryEntry.files], enabling traversal and lookup operations starting from this entry.
     */
    val root: DataDirectoryEntry
}