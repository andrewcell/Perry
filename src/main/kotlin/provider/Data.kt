package provider

import provider.wz.DataType

/**
 * Represents a node in a hierarchical data structure, typically used for parsing and accessing WZ file contents.
 *
 * Implements [DataEntity] to provide name-based identification and parent traversal,
 * and [Iterable<Data>] to enable iteration over child nodes.
 */
interface Data : DataEntity, Iterable<Data> {
    /**
     * The name of the data entity.
     */
    override val name: String
    /**
     * The data type of this [Data] instance, indicating the kind of value it holds.
     *
     * This property determines how [data] should be interpreted and processed.
     * For example,
     * - [DataType.STRING], [DataType.INT], [DataType.FLOAT], etc., indicate primitive-like values.
     * - [DataType.CANVAS] indicates the presence of image data accessible via [Canvas].
     * - [DataType.VECTOR], [DataType.CONVEX], [DataType.SOUND] represent specialized complex types.
     *
     * The value influences behavior in utilities such as those defined in [DataTool], particularly
     * when converting or extracting values from [data].
     */
    val type: DataType
    /**
     * The list of child [Data] objects contained within this [Data] node.
     *
     * This property provides access to the hierarchical structure of data entities,
     * where each child represents a subelement of the current node. The order of children
     * may be significant depending on the context in which the [Data] is used.
     */
    val children: List<Data>
    /**
     * Holds the actual data value represented by this [Data] node.
     *
     * The type of the value depends on the [type] property and may vary across different WZ file entries.
     * Common types include [String], [Int], [Double], [Float], [Point], or [Canvas].
     * When the entry does not hold a meaningful value, this property may be `null`.
     */
    val data: Any?
    /**
     * Retrieves a child [Data] element by traversing the hierarchy using a slash-separated path.
     *
     * The method navigates through the tree starting from the current instance, looking for a child
     * whose name matches each segment of the path in sequence. Returns `null` if any segment of the
     * path cannot be resolved or if the final target is not found.
     *
     * @param path A slash-separated string representing the hierarchical path to the desired child,
     * such as `"node1/node2/leaf"`.
     * @return The [Data] instance at the specified path, or `null` if the path is invalid or the
     * element does not exist.
     */
    fun getChildByPath(path: String): Data?
}