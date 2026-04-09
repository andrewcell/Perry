package provider

import java.awt.image.BufferedImage

/**
 * Represents a canvas containing an image with known dimensions.
 */
interface Canvas {
    /**
     * The height of the canvas in pixels.
     */
    val height: Int
    /**
     * The horizontal dimension of the canvas in pixels.
     */
    val width: Int
    /**
     * The underlying image data, or null if not available.
     *
     * When the [Canvas] instance contains image data (typically when [Data.type] is [provider.wz.DataType.CANVAS]),
     * this property holds the actual [BufferedImage]. Otherwise, it may be null.
     */
    val image: BufferedImage?
}