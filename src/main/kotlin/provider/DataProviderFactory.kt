package provider

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import provider.wz.WZFile
import provider.wz.XMLWZFile
import tools.ServerJSON.settings
import java.io.File

/**
 * Factory responsible for creating appropriate [DataProvider] instances based on the input file.
 *
 * Determines whether the input represents a WZ archive or an XML-based structure, and returns
 * a corresponding implementation: either [WZFile] for binary WZ files (with optional image support),
 * or [XMLWZFile] for directory-based XML structures.
 */
class DataProviderFactory {
    /**
     * Factory for creating [DataProvider] instances that abstract access to hierarchical data sources,
     * such as WZ archives or XML-based representations.
     *
     * Provides utility methods to load and instantiate appropriate data providers based on input file characteristics,
     * handling both native WZ files and fallback XML formats when necessary.
     */
    companion object {
        private val logger = KotlinLogging.logger {  }

        /**
         * The base file system path used to locate WZ data files.
         *
         * This value is sourced from application settings and serves as the root directory
         * for resolving relative paths when accessing WZ archives or related assets.
         */
        val wzPath = settings.wzPath

        /**
         * Creates and returns a [DataProvider] instance for the given input file.
         *
         * If the input file has a `.wz` extension (case-insensitive) and is not a directory,
         * attempts to load it as a WZ file. If loading fails, falls back to an XML-based
         * representation. Otherwise, treats the input as an XML-based WZ structure.
         *
         * @param input The file to be wrapped by a [DataProvider].
         * @param provideImages Determines whether image data should be included when loading WZ files;
         *        ignored for XML-based inputs.
         * @return A [DataProvider] instance backed either by a binary WZ file (if successfully loaded)
         *         or by an XML-based directory structure.
         */
        fun getWz(input: File, provideImages: Boolean): DataProvider {
            if (input.name.lowercase().endsWith("wz") && !input.isDirectory) {
                try {
                    return WZFile(input, provideImages)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load WZ file. Path: $wzPath" }
                }
            }
            return XMLWZFile(input)
        }

        /**
         * Creates and returns a [DataProvider] for the specified input file.
         *
         * Attempts to load the file as a WZ archive; if that fails or the file is not a valid WZ file,
         * falls back to an XML-based WZ representation.
         *
         * @param input The [File] to be used as the data source.
         */
        fun getDataProvider(input: File) = getWz(input, false)

        /**
         * Creates a [DataProvider] configured to provide image data.
         *
         * @param input The file to be used as the data source. Should be a `.wz` file or an XML-based WZ file.
         * @return A [DataProvider] instance capable of providing image data, either from a native WZ file or its XML fallback.
         */
        fun getImageProvidingDataProvider(input: File) = getWz(input, true)

        /**
         * Constructs a [File] instance pointing to the specified filename within the WZ data directory.
         *
         * @param filename The name of the file to locate relative to the WZ path.
         * @return A [File] object representing the resolved path to the requested file.
         */
        fun fileInWzPath(filename: String) = File(wzPath, filename)
    }
}