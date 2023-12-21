package tools

import mu.KLogging
import java.util.*
import kotlin.system.exitProcess

/**
 * The OpcodeProperties class is responsible for loading and managing opcode properties from a specified file.
 * It uses the Java Properties class to load key-value pairs from the file.
 * The loaded properties are stored in the prop variable and can be accessed using the getString method.
 * If the specified file cannot be loaded, an error message is logged and the program is terminated with a non-zero exit code.
 *
 * @property fileName The name of the file from which to load the opcode properties.
 * @property prop The Properties object in which the loaded opcode properties are stored.
 * @constructor Creates an OpcodeProperties object and attempts to load the opcode properties from the specified file.
 */
class OpcodeProperties(fileName: String) {
    val prop = Properties()

    /**
     * This initializer block attempts to load the opcode properties from the specified file.
     * If the file cannot be loaded, an error message is logged and the program is terminated with a non-zero exit code.
     *
     * @throws Exception if the specified file cannot be loaded.
     */
    init {
        try {
            prop.load(ResourceFile.loadStream(fileName))
        } catch (e: Exception) {
            logger.error(e) { "Failed to load opcode file: $fileName" }
            exitProcess(1)
        }
    }

    /**
     * This method attempts to retrieve the value of the specified key from the loaded opcode properties.
     * If the key does not exist in the properties, it returns null.
     *
     * @param key The key of the property to retrieve.
     * @return The value of the specified key, or null if the key does not exist in the properties.
     */
    fun getString(key: String) = if (!prop.containsKey(key)) null else prop.getProperty(key)

    companion object : KLogging()
}