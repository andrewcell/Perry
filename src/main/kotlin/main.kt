import net.server.Server
import tools.ServerJSON

fun main(args: Array<String>) {
    System.setProperty("polyglot.js.nashorn-compat", "true")
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")
    System.setProperty("configPath", args.firstOrNull() ?: "./settings.json")
    //val lc = LoggerFactory.getILoggerFactory() as LoggerContext
    System.setProperty("log.location", ServerJSON.settings.logging.directory)
    System.setProperty("log.level", ServerJSON.settings.logging.loggingLevel)
    System.setProperty("log.maxDays", ServerJSON.settings.logging.maxDaysToKeep.toString())
    Server.run()
}