package tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * FileDownloader is a utility object that provides methods for downloading files from a given URL.
 */
object FileDownloader {
    /**
     * Downloads a file from a given URL, unzips it and saves it to a specified path.
     *
     * @param url The URL of the file to download.
     * @param path The path where the downloaded file should be saved.
     * @param password The password for the zip file.
     */
    suspend fun run(url: String, path: String, password: String) {
        // Create a new HttpClient
        val client = HttpClient()
        // Send a GET request to the specified URL
        val res = client.request(url)
        // Create a ZipInputStream from the response body
        val zip = ZipInputStream(ByteArrayInputStream(res.body()))

        // TODO: Implement the rest of the method
    }
}