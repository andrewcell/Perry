package provider

import provider.wz.DataType
import java.awt.Point

class DataTool {
    companion object {
        fun getString(data: Data?) = data?.data?.toString()

        fun getString(data: Data?, def: String) = (data?.data?.toString()) ?: def

        fun getStringNullable(data: Data?, def: String?) = (data?.data?.toString()) ?: def

        fun getString(path: String, data: Data?) = getString(data?.getChildByPath(path))

        fun getString(path: String, data: Data?, def: String) = getString(data?.getChildByPath(path), def) ?: def

        fun getStringNullable(path: String, data: Data?, def: String?) = getStringNullable(data?.getChildByPath(path), def)

        fun getDouble(data: Data) = data.data as? Double

        fun getFloat(data: Data) = (data.data as? Float) ?: 0.0f

        fun getInt(data: Data?) = (data?.data as? Int) ?: 0

        fun getInt(path: String, data: Data?) = getInt(data?.getChildByPath(path))

        fun getInt(data: Data?, def: Int): Int {
            return if (data?.data == null) def
            else if (data.type == DataType.STRING) getString(data)?.toInt() ?: def
            else data.data as Int
        }

        fun getInt(path: String, data: Data?, def: Int) = getInt(data?.getChildByPath(path), def)

        fun getIntConvert(data: Data?) = if (data?.type == DataType.STRING) getString(data)?.toInt() else getInt(data)

        fun getIntConvert(path: String, data: Data?): Int {
            val d = data?.getChildByPath(path)
            return if (d?.type == DataType.STRING) {
                getString(d)?.toInt() ?: 0
            } else getInt(d)
        }

        fun getIntConvert(path: String, data: Data?, def: Int): Int {
            val d = data?.getChildByPath(path) ?: return def
            return if (d.type == DataType.STRING) {
                getString(d)?.toIntOrNull() ?: def
            } else {
                getInt(d, def)
            }
        }

        fun getImage(data: Data) = (data.data as? Canvas)?.image

        fun getPoint(data: Data?) = data?.data as? Point

        fun getPoint(path: String, data: Data) = getPoint(data.getChildByPath(path))

        fun getPoint(path: String, data: Data, def: Point): Point? {
            val pointData = data.getChildByPath(path) ?: return def
            return getPoint(pointData)
        }

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