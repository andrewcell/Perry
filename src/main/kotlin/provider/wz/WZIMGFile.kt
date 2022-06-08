package provider.wz

import mu.KLoggable
import tools.data.input.GenericSeekableLittleEndianAccessor
import tools.data.input.RandomAccessByteStream
import tools.data.input.SeekableLittleEndianAccessor
import java.awt.Point
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile

class WZIMGFile(wzFile: File, val file: WZFileEntry, val provideImages: Boolean, val modernImg: Boolean) : KLoggable {
    override val logger = logger()
    val root = file.parent?.let { WZIMGEntry(it, file.name, DataType.EXTENDED) }

    init {
        val raf = RandomAccessFile(wzFile, "r")
        val slea = GenericSeekableLittleEndianAccessor(RandomAccessByteStream(raf))
        slea.seek(file.offset.toLong())
        root?.let { parseExtended(it, slea, 0) }
        raf.close()
    }

    private fun parse(entry: WZIMGEntry, slea: SeekableLittleEndianAccessor) {
        var marker = slea.readByte()
        when (marker.toInt()) {
            0 -> entry.name = WZTool.readDecodedString(slea)
            1 -> entry.name = WZTool.readDecodedStringAtOffsetAndReset(slea, file.offset + slea.readInt())
        }
        marker = slea.readByte()
        when (marker.toInt()) {
            0 -> entry.type = DataType.IMG_0x00
            2, 11 -> {
                entry.type = DataType.SHORT
                entry.data = slea.readShort()
            }
            3 -> {
                entry.type = DataType.INT
                entry.data = WZTool.readValue(slea)
            }
            4 -> {
                entry.type = DataType.FLOAT
                entry.data = WZTool.readFloatValue(slea)
            }
            5 -> {
                entry.type = DataType.DOUBLE
                entry.data = slea.readDouble()
            }
            8 -> {
                entry.type = DataType.STRING
                val iMarker = slea.readByte()
                when (iMarker.toInt()) {
                    0 -> entry.data = WZTool.readDecodedString(slea)
                    1 -> entry.data = WZTool.readDecodedStringAtOffsetAndReset(slea, slea.readInt() + file.offset)
                }
            }
            9 -> {
                entry.type = DataType.EXTENDED
                var endOfExtendedBlock = slea.readInt()
                endOfExtendedBlock += slea.position.toInt()
                parseExtended(entry, slea, endOfExtendedBlock.toLong())
            }
        }
    }

    fun parseExtended(entry: WZIMGEntry, slea: SeekableLittleEndianAccessor, endOfExtendedBlock: Long) {
        var marker = slea.readByte()
        val type = when (marker.toInt()) {
            0x73 -> WZTool.readDecodedString(slea)
            0x1B -> WZTool.readDecodedStringAtOffsetAndReset(slea, file.offset + slea.readInt())
            else -> ""
        }
        when (type) {
            "Property" -> {
                entry.type = DataType.PROPERTY
                slea.readByte()
                slea.readByte()
                val children = WZTool.readValue(slea)
                for (i in 0 until children) {
                    val cEntry = WZIMGEntry(entry, entry.name, DataType.PROPERTY)
                    parse(cEntry, slea)
                    //cEntry.finish();
                    entry.addChild(cEntry)
                }
            }
            "Canvas" -> {
                entry.type = DataType.CANVAS
                slea.readByte()
                marker = slea.readByte()
                if (marker.toInt() == 1) {
                    slea.readByte()
                    slea.readByte()
                    val children = WZTool.readValue(slea)
                    for (i in 0 until children) {
                        val child = WZIMGEntry(entry, entry.name, DataType.CANVAS)
                        parse(child, slea)
                        //child.finish();
                        entry.addChild(child)
                    }
                }
                val width = WZTool.readValue(slea)
                val height = WZTool.readValue(slea)
                val format = WZTool.readValue(slea)
                val format2 = slea.readByte().toInt()
                slea.readInt()
                val dataLength = slea.readInt() - 1
                slea.readByte()
                if (provideImages) {
                    val pngData = slea.read(dataLength)
                    entry.data = PNGCanvas(width, height, dataLength, format + format2, pngData)
                } else {
                    entry.data = PNGCanvas(width, height, dataLength, format + format2, null)
                    slea.skip(dataLength)
                }
            }
            "Shape2D#Vector2D" -> {
                entry.type = DataType.VECTOR
                val x = WZTool.readValue(slea)
                val y = WZTool.readValue(slea)
                entry.data = Point(x, y)
            }
            "Shape2D#Convex2D" -> {
                val children = WZTool.readValue(slea)
                for (i in 0 until children) {
                    val cEntry = WZIMGEntry(entry, entry.name, DataType.CONVEX)
                    parseExtended(cEntry, slea, 0)
                    //cEntry.finish();
                    entry.addChild(cEntry)
                }
            }
            "Sound_DX8" -> {
                entry.type = DataType.SOUND
                slea.readByte()
                val dataLength = WZTool.readValue(slea)
                WZTool.readValue(slea) // no clue what this is
                val offset = slea.position.toInt()
                entry.data = ImgSound(dataLength, offset - file.offset)
                slea.seek(endOfExtendedBlock)
            }
            "UOL" -> {
                entry.type = DataType.UOL
                slea.readByte()
                val uolMarker = slea.readByte().toInt()
                when (uolMarker) {
                    0 -> entry.data = WZTool.readDecodedString(slea)
                    1 -> entry.data = WZTool.readDecodedStringAtOffsetAndReset(slea, file.offset + slea.readInt())
                    else -> logger.error { "Unknown UOL marker: $uolMarker ${entry.name}" }
                }
            }
        }
    }

    fun dumpImg(out: OutputStream, slea: SeekableLittleEndianAccessor) {
        val os = DataOutputStream(out)
        val oldPos = slea.position
        slea.seek(file.offset.toLong())
        for (x in 0 until file.size) {
            os.write(slea.readByte().toInt())
        }
        slea.seek(oldPos)
    }
}