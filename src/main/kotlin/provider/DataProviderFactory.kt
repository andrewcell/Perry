package provider

import io.ktor.client.*
import io.ktor.client.request.*
import mu.KLogging
import provider.wz.WZFile
import provider.wz.XMLWZFile
import tools.ServerJSON.settings
import java.io.File

class DataProviderFactory {
    companion object : KLogging() {
        val wzPath = settings.wzPath

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

        fun getDataProvider(input: File) = getWz(input, false)

        fun getImageProvidingDataProvider(input: File) = getWz(input, true)

        fun fileInWzPath(filename: String) = File(wzPath, filename)
    }
}