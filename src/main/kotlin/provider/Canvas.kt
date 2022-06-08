package provider

import java.awt.image.BufferedImage

interface Canvas {
    val height: Int
    val width: Int
    val image: BufferedImage?
}