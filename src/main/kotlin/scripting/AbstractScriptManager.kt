package scripting

import client.Client
import java.io.File
import java.io.FileReader


abstract class AbstractScriptManager {
    fun getScriptFileReader(path: String): FileReader? {
        val file = File("scripts/$path")
        return if (file.exists()) FileReader(file, charset("x-windows-949"))//TODO: convert script files to utf-8. not like this.
        else null
    }

    companion object {
        const val nashornCompat = "load('nashorn:mozilla_compat.js');"
    }

    fun resetContext(path: String, c: Client) = c.removeScriptEngine("scripts/$path")
}