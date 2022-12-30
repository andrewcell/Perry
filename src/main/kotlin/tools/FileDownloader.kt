package tools

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object FileDownloader {
    suspend fun run(url: String, path: String, password: String) {
        val client = HttpClient()
        val res = client.request(url)
        val zip = ZipInputStream(ByteArrayInputStream(res.body()))

    }
}