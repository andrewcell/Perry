package provider

import provider.wz.DataType
import java.awt.Point

/**
 * Utility class providing helper methods for extracting typed values from [Data] objects.
 *
 * This class offers convenient methods to retrieve strings, integers, doubles, floats,
 * points, and images from hierarchical data structures, with support for default values
 * and path-based navigation.
 */
class DataTool {
    companion object {
        /**
         * Extracts a string value from the given data object.
         *
         * @param data The data object to extract the string from
         * @return The string representation of the data, or `null` if data is null
         */
        fun getString(data: Data?) = data?.data?.toString()

        /**
         * Extracts a string value from the given data object with a default fallback.
         *
         * @param data The data object to extract the string from
         * @param def The default value to return if data is null
         * @return The string representation of the data, or [def] if data is null
         */
        fun getString(data: Data?, def: String) = (data?.data?.toString()) ?: def

        /**
         * Extracts a string value from the given data object with a nullable default.
         *
         * @param data The data object to extract the string from
         * @param def The default value to return if data is null (can be null)
         * @return The string representation of the data, or [def] if data is null
         */
        fun getStringNullable(data: Data?, def: String?) = (data?.data?.toString()) ?: def

        /**
         * Extracts a string value from a child at the specified path.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @return The string value at the path, or `null` if not found
         */
        fun getString(path: String, data: Data?) = getString(data?.getChildByPath(path))

        /**
         * Extracts a string value from a child at the specified path with a default fallback.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @param def The default value to return if the path doesn't exist
         * @return The string value at the path, or [def] if not found
         */
        fun getString(path: String, data: Data?, def: String) = getString(data?.getChildByPath(path), def) ?: def

        /**
         * Extracts a string value from a child at the specified path with a nullable default.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @param def The default value to return if the path doesn't exist (can be null)
         * @return The string value at the path, or [def] if not found
         */
        fun getStringNullable(path: String, data: Data?, def: String?) = getStringNullable(data?.getChildByPath(path), def)

        /**
         * Extracts a double value from the given data object.
         *
         * @param data The data object to extract the double from
         * @return The double value, or `null` if the data is not a double
         */
        fun getDouble(data: Data) = data.data as? Double

        /**
         * Extracts a float value from the given data object.
         *
         * @param data The data object to extract the float from
         * @return The float value, or `0.0f` if the data is not a float
         */
        fun getFloat(data: Data) = (data.data as? Float) ?: 0.0f

        /**
         * Extracts an integer value from the given data object.
         *
         * @param data The data object to extract the integer from
         * @return The integer value, or `0` if data is null or not an integer
         */
        fun getInt(data: Data?) = (data?.data as? Int) ?: 0

        /**
         * Extracts an integer value from a child at the specified path.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @return The integer value at the path, or `0` if not found
         */
        fun getInt(path: String, data: Data?) = getInt(data?.getChildByPath(path))

        /**
         * Extracts an integer value from the given data object with a default fallback.
         * Handles string-type data by parsing it as an integer.
         *
         * @param data The data object to extract the integer from
         * @param def The default value to return if data is null or cannot be converted
         * @return The integer value, or [def] if extraction fails
         */
        fun getInt(data: Data?, def: Int): Int {
            return if (data?.data == null) def
            else if (data.type == DataType.STRING) getString(data)?.toInt() ?: def
            else data.data as Int
        }

        /**
         * Extracts an integer value from a child at the specified path with a default fallback.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @param def The default value to return if the path doesn't exist
         * @return The integer value at the path, or [def] if not found
         */
        fun getInt(path: String, data: Data?, def: Int) = getInt(data?.getChildByPath(path), def)

        /**
         * Extracts an integer value, converting from string if necessary.
         *
         * @param data The data object to extract from
         * @return The integer value, converting from string if the data type is STRING
         */
        fun getIntConvert(data: Data?) = if (data?.type == DataType.STRING) getString(data)?.toInt() else getInt(data)

        /**
         * Extracts an integer value from a child path, converting from string if necessary.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @return The integer value, or `0` if not found or conversion fails
         */
        fun getIntConvert(path: String, data: Data?): Int {
            val d = data?.getChildByPath(path)
            return if (d?.type == DataType.STRING) {
                getString(d)?.toInt() ?: 0
            } else getInt(d)
        }

        /**
         * Extracts an integer value from a child path with a default, converting from string if necessary.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @param def The default value to return if extraction fails
         * @return The integer value, or [def] if not found or conversion fails
         */
        fun getIntConvert(path: String, data: Data?, def: Int): Int {
            val d = data?.getChildByPath(path) ?: return def
            return if (d.type == DataType.STRING) {
                getString(d)?.toIntOrNull() ?: def
            } else {
                getInt(d, def)
            }
        }

        /**
         * Extracts an image from a canvas data object.
         *
         * @param data The data object containing canvas data
         * @return The buffered image, or `null` if the data is not a canvas
         */
        fun getImage(data: Data) = (data.data as? Canvas)?.image

        /**
         * Extracts a point value from the given data object.
         *
         * @param data The data object to extract the point from
         * @return The point value, or `null` if data is null or not a point
         */
        fun getPoint(data: Data?) = data?.data as? Point

        /**
         * Extracts a point value from a child at the specified path.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @return The point value at the path, or `null` if not found
         */
        fun getPoint(path: String, data: Data) = getPoint(data.getChildByPath(path))

        /**
         * Extracts a point value from a child at the specified path with a default fallback.
         *
         * @param path The path to the child data element
         * @param data The parent data object to search from
         * @param def The default point to return if the path doesn't exist
         * @return The point value at the path, or [def] if not found
         */
        fun getPoint(path: String, data: Data, def: Point): Point? {
            val pointData = data.getChildByPath(path) ?: return def
            return getPoint(pointData)
        }

        /**
         * Constructs the full hierarchical path to a data element.
         *
         * @param data The data element to get the path for
         * @return The full path from root to the data element, with segments separated by `/`
         */
        fun getFullDataPath(data: Data): String {
            var path = ""
            var myData: DataEntity? = data
            while (myData != null) {
                path = myData.name + "/" + path
                myData = myData.parent
            }
            return path.substring(0, path.length - 1)
        }
    }
}