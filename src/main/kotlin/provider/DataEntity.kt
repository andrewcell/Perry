package provider

/**
 * Represents an entity in a hierarchical data structure that can be identified by a name and has an optional parent.
 *
 * Provides basic navigation capabilities for traversing the hierarchy upward through the [parent] reference.
 */
interface DataEntity {
    /**
     * The name of the data entity.
     */
    val name: String
    /**
     * The parent node in the hierarchical data structure, or `null` if this entity is the root.
     *
     * This property enables upward traversal of the tree, allowing navigation from any child node
     * to its immediate parent. It is instrumental in reconstructing full paths (as seen in [DataTool.getFullDataPath])
     * and for context-aware operations that require knowledge of an entity's position within the hierarchy.
     */
    val parent: DataEntity?
}