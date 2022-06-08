package provider.wz

import mu.KLoggable
import provider.Canvas
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class FileStoredPngCanvas(override var width: Int, override var height: Int, val file: File) : Canvas, KLoggable {
    override val logger = logger()

    override var image: BufferedImage? = null
        get() {
            loadImageIfNecessary()
            return field
        }

    private fun loadImageIfNecessary() {
        if (image == null) {
            try {
                image = ImageIO.read(file)
                width = image?.width ?: return
                height = image?.height ?: return
            } catch (e: Exception) {
                logger.error(e) { "Error caused when loading image. Path: ${file.absolutePath}" }
            }
        }
    }

}