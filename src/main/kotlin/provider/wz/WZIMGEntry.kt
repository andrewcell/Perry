package provider.wz

import provider.Data
import provider.DataEntity

class WZIMGEntry(override val parent: DataEntity, override var name: String, override var type: DataType) : Data {
    override val children: MutableList<Data> = mutableListOf()
    override var data: Any? = null

    override fun getChildByPath(path: String): Data? {
        val segments = path.split("/")
        if (segments[0] == "..") {
            return (parent as? Data)?.getChildByPath(path.substring(path.indexOf('/') + 1))
        }
        var ret: Data = this
        for (x in segments.indices) {
            var foundChild = false
            for (child in ret.children) {
                if (child.name == segments[x]) {
                    ret = child
                    foundChild = true
                    break
                }
            }
            if (!foundChild) return null
        }
        return ret
    }

    fun addChild(entry: WZIMGEntry) = children.add(entry)

    override fun iterator() = children.iterator()
}