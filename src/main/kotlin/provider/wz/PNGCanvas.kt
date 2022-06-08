package provider.wz

import provider.Canvas
import java.awt.Point
import java.awt.image.*
import java.util.zip.Inflater
import kotlin.experimental.and
import kotlin.experimental.or

class PNGCanvas(override val width: Int, override val height: Int, val dataLength: Int, val format: Int, val data: ByteArray?) : Canvas {
    override val image: BufferedImage
        get() {
            var size8888 = 0
            var maxWriteBuf = 2
            var maxHeight = 3
            var writeBuf = ByteArray(maxWriteBuf)
            var rowPointers = ByteArray(maxHeight)
            val sizeUncompressed = when (format) {
                1, 513 ->  height * width * 4
                2 -> height * width * 8
                517 -> height * width / 128
                else -> 0
            }
            size8888 = height * width * 8
            if (size8888 > maxWriteBuf) {
                maxWriteBuf = size8888
                writeBuf = ByteArray(maxWriteBuf)
            }
            if (height > maxHeight) {
                maxHeight = height
                rowPointers = ByteArray(maxHeight)
            }
            val dec = Inflater()
            dec.setInput(data, 0, dataLength)
            val uc = ByteArray(sizeUncompressed)
            var decLen = dec.inflate(uc)
            dec.end()
            when (format) {
                1 -> {
                    for (i in 0 until sizeUncompressed) {
                        val low: Byte = uc[i] and 0x0F
                        val high: Byte = uc[i] and 0xF0.toByte()
                        writeBuf[i shl 1] = (low.toInt() shl 4 or low.toInt() and 0xFF).toByte()
                        writeBuf[(i shl 1) + 1] = (high or ((high.toInt() ushr 4 and 0xF).toByte()))
                    }
                }
                2 -> writeBuf = uc
                513 -> {
                    for (i in 0 until decLen step 2) {
                        val bBits: Byte = ((uc[i] and 0x1F).toInt() shl 3).toByte()
                        val gBits: Byte = ((uc[i + 1] and 0x07).toInt() shl 5 or ((uc[i] and 0xE0.toByte()).toInt() shr 3)).toByte()
                        val rBits = uc[i + 1] and 0xF8.toByte()
                        writeBuf[i shl 1] = bBits or (bBits.toInt() shr 5).toByte()
                        writeBuf[(i shl 1) + 1] = gBits or (gBits.toInt() shr 6).toByte()
                        writeBuf[(i shl 1) + 2] = rBits or (rBits.toInt() shr 5).toByte()
                        writeBuf[(i shl 1) + 3] = 0xFF.toByte()
                    }
                }
                517 -> {
                    var b: Byte = 0x00
                    var pixelIndex = 0
                    for (i in 0 until decLen) {
                        for (j in 0..7) {
                            b = ((uc[i].toInt() and (0x01 shl 7 - j) shr 7 - j) * 255).toByte()
                            for (k in 0..15) {
                                pixelIndex = (i shl 9) + (j shl 6) + k * 2
                                writeBuf[pixelIndex] = b
                                writeBuf[pixelIndex + 1] = b
                                writeBuf[pixelIndex + 2] = b
                                writeBuf[pixelIndex + 3] = 0xFF.toByte()
                            }
                        }
                    }
                }
            }
            val imgData = DataBufferByte(writeBuf, sizeUncompressed)
            val sm = PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, 4, width * 4, ZAHLEN)
            val imgRaster = Raster.createWritableRaster(sm, imgData, Point(0, 0))
            val aa = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            aa.data = imgRaster
            return aa
        }

    companion object {
        private val ZAHLEN = intArrayOf(2, 1, 0, 3)
    }
}