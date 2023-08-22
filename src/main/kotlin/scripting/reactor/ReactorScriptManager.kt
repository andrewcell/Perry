package scripting.reactor

import client.Client
import kotlinx.serialization.json.Json
import mu.KLoggable
import scripting.AbstractScriptManager
import server.maps.Reactor
import server.maps.ReactorDropEntry
import tools.ResourceFile
import tools.settings.ReactorDropDatabase
import javax.script.Invocable
import javax.script.ScriptEngineManager

object ReactorScriptManager : AbstractScriptManager(), KLoggable {
    override val logger = logger()
    val drops = mutableMapOf<Int, List<ReactorDropEntry>>()

    fun act(c: Client, reactor: Reactor) {
        try {
            val rm = ReactorActionManager(c, reactor)
            val engine = ScriptEngineManager().getEngineByName("graal.js")
            engine?.eval(nashornCompat)
            val file = getScriptFileReader("reactor/${reactor.rid}.js") ?: return
            engine?.put("rm", rm)
            engine?.eval(file)
            (engine as Invocable?)?.invokeFunction("act")
        } catch (e: Exception) {
            logger.error(e) { "Failed to register reactor script. ReactorId: ${reactor.rid}" }
        }
    }

    fun getDrops(id: Int): List<ReactorDropEntry> {
        var ret = drops[id]
        if (ret == null) {
            val reactorDropData = ResourceFile.load("ReactorDrops.json")
                ?.let { Json.decodeFromString<Array<ReactorDropDatabase>>(it) } ?: return emptyList()
            val retT = mutableListOf<ReactorDropEntry>()
            reactorDropData.forEach {
                retT.add(ReactorDropEntry(it.itemId, it.change, it.questId))
            }
            ret = retT
            drops[id] = ret
        }
        return ret
    }

    fun touch(c: Client, reactor: Reactor) = touching(c, reactor, true)

    fun untouch(c: Client, reactor: Reactor) = touching(c, reactor, false)

    fun touching(c: Client, reactor: Reactor, touching: Boolean) {
        try {
            val rm = ReactorActionManager(c, reactor)
            val engine = ScriptEngineManager().getEngineByName("graal.js")
            engine?.eval(nashornCompat)
            val file = getScriptFileReader("reactor/${reactor.rid}.js") ?: return
            engine?.put("rm", rm)
            engine?.eval(file)
            val iv = engine as Invocable?
            if (touching) {
                iv?.invokeFunction("touch")
            } else {
                iv?.invokeFunction("untouch")
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle touch reactor." }
        }
    }
}