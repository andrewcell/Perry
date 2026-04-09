package provider

/**
 * Represents a leaf or atomic data entry within a hierarchical structure, providing metadata about its location and integrity.
 *
 * Extends [DataEntity] to inherit naming and hierarchical positioning capabilities,
 * and serves as a contract for entities that carry raw data with associated structural information such as size,
 * position offset, and checksum—typically used for verification and efficient access in binary data formats like WZ files.
 */
interface DataEntry : DataEntity {
    /**
     * The name of the data entity.
     *
     * This property provides a unique identifier within its scope in the hierarchical structure.
     * It is used for navigation, lookups, and path construction (e.g., via [Data.getChildByPath] or [DataTool.getFullDataPath]).
     */
    override val name: String
    /**
     * The size of the data entry in bytes.
     *
     * This property represents the total byte count occupied by the data entity,
     * typically reflecting its serialized or stored length within the underlying data source.
     */
    val size: Int
    /**
     * A numerical checksum value associated with the data entry, typically used for integrity verification.
     *
     * This property represents a hash or checksum computed from the entry's contents, allowing detection of
     * corruption or modification. The exact algorithm and semantics depend on the underlying WZ file format.
     */
    val checksum: Int
    /**
     * The byte offset within the data source where this entry's content begins.
     *
     * This value indicates the starting position of the entry's raw data relative to the beginning of the file or stream,
     * and is typically used for efficient random-access reading without loading the entire content into memory.
     */
    val offset: Int
}