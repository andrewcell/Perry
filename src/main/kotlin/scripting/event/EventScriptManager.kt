package scripting.event

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import mu.KLogging
import net.server.channel.Channel
import scripting.AbstractScriptManager
import javax.script.Invocable
import javax.script.ScriptContext.ENGINE_SCOPE
import javax.script.ScriptEngineManager

class EventScriptManager(channelServer: Channel, scripts: Array<String>) : AbstractScriptManager() {
    data class EventEntry(val iv: Invocable, val em: EventManager)

    val events = mutableMapOf<String, EventEntry>()

    init {
        scripts.forEach {
            if (it != "") {
                try {
                    val file = getScriptFileReader("event/$it.js") ?: return@forEach
                    val engine = ScriptEngineManager().getEngineByName("graal.js") as GraalJSScriptEngine
                    val binding = engine.getBindings(ENGINE_SCOPE)
                    binding["polyglot.js.nashorn-compat"] = true
                    val em =  EventManager(channelServer, engine, it)
                    binding["em"] = em
                    engine.eval(nashornCompat)
                    engine.eval(file)
                    events[it] = EventEntry(engine, em)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to register event script. Name: $it" }
                }
            }
        }
    }

    fun getEventManager(event: String) = events[event]?.em

    fun init() {
        events.values.forEach {
            try {
                //(it.iv as ScriptEngine).put("em", it.em)
                it.iv.invokeFunction("init", null)
            } catch (e: Exception) {
                logger.error(e) { "Error caused when init events." }
            }
        }
    }

    fun cancel() = events.values.forEach { it.em.cancel() }

    companion object : KLogging()
}