package tools

import mu.KLoggable
import tools.ResourceFile.logger
import java.net.URL

/**
 * Files from resources folder handler
 *
 * Help to load the file from resources folder in project.
 *
 * @property logger Logger
 * @author A. S. Choe
 * @since 1.0.0
 */
object ResourceFile : KLoggable {
    override val logger = logger()

    /**
     * Return path of the file
     *
     * @param fileName File name to load.
     * @return Loadable path of a file. If null returned from API, just return filename.
     */
    fun getPath(fileName: String) = this.javaClass.classLoader.getResource(fileName)?.path ?: fileName

    /**
     * Return content of the file
     *
     * @param fileName File name to load.
     * @return Content of the file. If not exists, and return null.
     */
    fun load(fileName: String) = getFile(fileName)?.readText()

    /**
     * Return InputStream object of the file
     *
     * @param fileName File name to load.
     * @return InputStream object of the file. If not exists, return null.
     */
    fun loadStream(fileName: String) = getFile(fileName)?.openStream()

    /**
     * Return URL object of the file
     *
     * @param fileName File name to load.
     * @return URL object of the file. If not exists, print warning log and return null.
     */
    private fun getFile(fileName: String): URL? {
        val file = this.javaClass.classLoader.getResource(fileName)
        return if (file == null) {
            logger.warn { "Failed to load resource file $fileName." }
            null
        } else file
    }
}