package tools

import mu.KLogging
import java.util.*
import kotlin.system.exitProcess

class OpcodeProperties(fileName: String) {
    val prop = Properties()
    init {
        try {
            prop.load(ResourceFile.loadStream(fileName))
        } catch (e: Exception) {
            logger.error(e) { "Failed to load opcode file: $fileName" }
            exitProcess(1)
        }
    }

    fun getString(key: String) = if (!prop.containsKey(key)) null else prop.getProperty(key)

    companion object : KLogging()
}