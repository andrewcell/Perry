package provider.wz

import org.w3c.dom.Node
import provider.Data
import provider.DataEntity
import java.awt.Point
import java.io.File
import java.io.FileInputStream
import javax.xml.parsers.DocumentBuilderFactory

class XMLDomData : Data {
    private val node: Node
    private var imageDataDir: File? = null

    constructor(fis: FileInputStream, imageDataDir: File) {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = documentBuilder.parse(fis)
        node = document.firstChild
        this.imageDataDir = imageDataDir
    }

    constructor(node: Node) {
        this.node = node
    }

    override val parent: DataEntity?
        get() {
            val parentNode = node.parentNode
            if (parentNode.nodeType == Node.DOCUMENT_NODE) return null
            val parentData = XMLDomData(parentNode)
            parentData.imageDataDir = imageDataDir?.parentFile
            return parentData
        }

    override val type: DataType
        get() {
            return when (node.nodeName) {
                "imgdir" -> DataType.PROPERTY
                "canvas" -> DataType.CANVAS
                "convex" -> DataType.CONVEX
                "sound" -> DataType.SOUND
                "uol" -> DataType.UOL
                "double" -> DataType.DOUBLE
                "float" -> DataType.FLOAT
                "int" -> DataType.INT
                "short" -> DataType.SHORT
                "string" -> DataType.STRING
                "vector" -> DataType.VECTOR
                "null" -> DataType.IMG_0x00
                else -> DataType.UNKNOWN_TYPE
            }
        }

    override val data: Any?
        get() {
            val attributes = node.attributes
            when (type) {
                DataType.DOUBLE, DataType.FLOAT, DataType.INT, DataType.SHORT, DataType.STRING, DataType.UOL -> {
                    val value = attributes.getNamedItem("value").nodeValue
                    when (type) {
                        DataType.DOUBLE -> return java.lang.Double.valueOf(value.toDouble())
                        DataType.FLOAT -> return java.lang.Float.valueOf(value.toFloat())
                        DataType.INT -> return Integer.valueOf(value.toInt())
                        DataType.SHORT -> return value.toShort()
                        DataType.STRING, DataType.UOL -> return value
                        else -> {}
                    }
                }
                DataType.VECTOR -> {
                    val x = attributes.getNamedItem("x").nodeValue.toInt()
                    val y = attributes.getNamedItem("y").nodeValue.toInt()
                    return Point(x, y)
                }
                DataType.CANVAS -> {
                    val width = attributes.getNamedItem("width").nodeValue.toInt()
                    val height = attributes.getNamedItem("height").nodeValue.toInt()
                    return FileStoredPngCanvas(width, height, File(imageDataDir, "$name.png"))
                }
                else -> {}
            }
            return null
        }

    override fun getChildByPath(path: String): Data? {
        val segments = path.split('/')
        if (segments[0] == "..") {
            return (parent as Data).getChildByPath(path.substring(path.indexOf("/") + 1))
        }
        var myNode = node
        for (x in segments.indices) {
            val childNodes = myNode.childNodes
            var foundChild = false
            for (i in 0 until childNodes.length) {
                val childNode = childNodes.item(i)
                if (childNode.nodeType == Node.ELEMENT_NODE && childNode.attributes.getNamedItem("name").nodeValue == segments[x]) {
                    myNode = childNode
                    foundChild = true
                    break
                }
            }
            if (!foundChild) {
                return null
            }
        }
        val ret = XMLDomData(myNode)
        ret.imageDataDir = File(imageDataDir, "$name/$path").parentFile
        return ret
    }

    override val children: List<Data>
        get() {
            val ret = mutableListOf<Data>()
            val childNodes = node.childNodes
            for (i in 0 until childNodes.length) {
                val childNode = childNodes.item(i)
                if (childNode.nodeType == Node.ELEMENT_NODE) {
                    val child = XMLDomData(childNode)
                    child.imageDataDir = File(imageDataDir, name)
                    ret.add(child)
                }
            }
            return ret
        }

    override val name: String
        get() = this.node.attributes.getNamedItem("name").nodeValue

    override fun iterator() = children.iterator()
}