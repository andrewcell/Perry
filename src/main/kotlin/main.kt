import kotlinx.coroutines.*
import net.server.Server
import tools.CoroutineManager
import tools.FileDownloader
import tools.ServerJSON
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    val command = args.getOrNull(0) ?: ""
    when (command) {
        "download" -> { // ./Perry download http://remote.local/perry Password! settings.json download [url] [password] [settings file]
            System.setProperty("configPath", args[5] ?: "./settings.json")
            CoroutineScope(Dispatchers.IO).launch {
                if (Files.notExists(Paths.get(ServerJSON.settings.wzPath))) {
                    FileDownloader.run(args[1] + "/wz.zip", ServerJSON.settings.wzPath, args[2])
                }
                if (Files.notExists(Paths.get("/scripts"))) {
                    FileDownloader.run(args[1] + "/scripts.zip", "/scripts", args[2])
                }
            }
            return
        }
    }
    System.setProperty("polyglot.js.nashorn-compat", "true")
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")
    System.setProperty("configPath", args.firstOrNull() ?: "./settings.json")
    //val lc = LoggerFactory.getILoggerFactory() as LoggerContext
    System.setProperty("log.location", ServerJSON.settings.logging.directory)
    System.setProperty("log.level", ServerJSON.settings.logging.loggingLevel)
    System.setProperty("log.maxDays", ServerJSON.settings.logging.maxDaysToKeep.toString())

    Server.run()
}