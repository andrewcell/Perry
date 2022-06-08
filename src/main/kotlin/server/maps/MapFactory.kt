package server.maps

import com.beust.klaxon.Klaxon
import database.PlayerNpcs
import mu.KLogging
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import provider.Data
import provider.DataProvider
import provider.DataTool
import server.PortalFactory
import server.life.LifeFactory
import server.life.Monster
import tools.ResourceFile
import tools.settings.WZCustomLifeDatabase
import java.awt.Point
import java.awt.Rectangle
import java.sql.SQLException

class MapFactory(val source: DataProvider, stringSource: DataProvider, val world: Int, var channel: Int) {
    private val nameData = stringSource.getData("Map.img")
    val maps = mutableMapOf<Int, GameMap>()

    fun getMap(mapId: Int): GameMap {
        var map = maps[mapId]
        if (map == null) {
            synchronized(this) {
                val oMap = maps[mapId]
                if (oMap != null) return oMap
                var mapName = getMapName(mapId)
                var mapData = source.getData(mapName)
                val link = DataTool.getString(mapData?.getChildByPath("info/link"), "")
                if (link != "") { // nx made hundreds of dojo maps so to reduce the size they added links.
                    mapName = getMapName(link.toInt())
                    mapData = source.getData(mapName)
                }
                val monsterRate = (mapData?.getChildByPath("info/mobRate")?.data ?: 0.0f) as Float
                val map1 = GameMap(mapId, world, channel, DataTool.getInt("info/returnMap", mapData), monsterRate)
                map1.onFirstUserEnter = DataTool.getString(mapData?.getChildByPath("info/onFirstUserEnter"), mapId.toString())
                map1.onUserEnter = DataTool.getString(mapData?.getChildByPath("info/onUserEnter"), mapId.toString())
                map1.fieldLimit = DataTool.getInt(mapData?.getChildByPath("info/fieldLimit"), 0)
                map1.mobInterval = DataTool.getInt(mapData?.getChildByPath("info/createMobInterval"), 8000)
                val portalFactory = PortalFactory()
                mapData?.getChildByPath("portal")?.forEach {
                    map1.addPortal(portalFactory.makePortal(DataTool.getInt(it.getChildByPath("pt")), it))
                }
                val timeMob = mapData?.getChildByPath("info/timeMob")
                if (timeMob != null) {
                    map1.timeMob = Pair(DataTool.getInt(timeMob.getChildByPath("id")), DataTool.getString(timeMob.getChildByPath("message")))
                }
                val allFootholds = mutableListOf<Foothold>()
                val lb = Point()
                val ub = Point()
                mapData?.getChildByPath("foothold")?.forEach { footRoot ->
                    footRoot.forEach { footCat ->
                        footCat.forEach { footHold ->
                            val x1 = DataTool.getInt(footHold.getChildByPath("x1"))
                            val y1 = DataTool.getInt(footHold.getChildByPath("y1"))
                            val x2 = DataTool.getInt(footHold.getChildByPath("x2"))
                            val y2 = DataTool.getInt(footHold.getChildByPath("y2"))
                            val fh = Foothold(Point(x1, y1), Point(x2, y2), footHold.name.toInt())
                            fh.prev = DataTool.getInt(footHold.getChildByPath("prev"))
                            fh.next = DataTool.getInt(footHold.getChildByPath("next"))
                            if (fh.p1.x < lb.x) lb.x = fh.p1.x
                            if (fh.p2.x > ub.x) ub.x = fh.p2.x
                            if (fh.p1.y < lb.y) lb.y = fh.p1.y
                            if (fh.p2.y > ub.y) ub.y = fh.p2.y
                            allFootholds.add(fh)
                        }
                    }
                }
                val fTree = FootholdTree(lb, ub)
                allFootholds.forEach { fTree.insert(it) }
                map1.footholds = fTree
                val area = mapData?.getChildByPath("area")
                area?.forEach {
                    val x1 = DataTool.getInt(it.getChildByPath("x1"))
                    val y1 = DataTool.getInt(it.getChildByPath("y1"))
                    val x2 = DataTool.getInt(it.getChildByPath("x2"))
                    val y2 = DataTool.getInt(it.getChildByPath("y2"))
                    map1.addArea(Rectangle(x1, y1, (x2 - x1), (y2 - y1)))
                }
                try {
                    transaction {
                        PlayerNpcs.select { PlayerNpcs.map eq mapId }.forEach {
                            map1.addMapObject(PlayerNPCs(
                                rowId = it[PlayerNpcs.id],
                                npcId = it[PlayerNpcs.scriptId],
                                name = it[PlayerNpcs.name],
                                cy = it[PlayerNpcs.cy],
                                hair = it[PlayerNpcs.hair],
                                face = it[PlayerNpcs.face],
                                skin = it[PlayerNpcs.skin].toByte(),
                                fh = it[PlayerNpcs.foothold],
                                rx0 = it[PlayerNpcs.rx0],
                                rx1 = it[PlayerNpcs.rx1],
                                x = it[PlayerNpcs.x]))
                        }
                    }
                } catch (e: SQLException) {
                    logger.error(e) { "Failed to get player NPCs." }
                }
                mapData?.getChildByPath("life")?.forEach { life ->
                    var id = DataTool.getString(life.getChildByPath("id"))
                    val type = DataTool.getString(life.getChildByPath("type"))
                    if (id == "9001105") {
                        id = "9001108"
                    }
                    val myLife = loadLife(life, id, type)
                    if (myLife is Monster) {
                        val mobTime = DataTool.getInt("mobTime", life, 0)
                        val team = DataTool.getInt("team", life, -1)
                        if (mobTime == -1) map1.spawnMonster(myLife, true) else map1.addMonsterSpawn(
                            myLife,
                            mobTime,
                            team
                        )
                    } else {
                        if (myLife != null) {
                            map1.addMapObject(myLife)
                        }
                    }
                }
                val reactor = mapData?.getChildByPath("reactor")
                reactor?.forEach { r ->
                    DataTool.getString(r.getChildByPath("id"))?.let {
                        map1.spawnReactor(loadReactor(r, it))
                    }
                }
                try {
                    map1.mapName = DataTool.getString("mapName", nameData?.getChildByPath(getMapStringName(mapId)), "")
                    map1.streetName =
                        DataTool.getString("streetName", nameData?.getChildByPath(getMapStringName(mapId)), "")
                } catch (e: Exception) {
                    map1.mapName = ""
                    map1.streetName = ""
                }
                val custom = customLife[mapId]
                custom?.forEach { n ->
                    if (n.cType == "n") map1.addMapObject(n)
                    else if (n.cType == "m") map1.addMonsterSpawn((n as Monster), n.mTime, -1)
                }
                map1.clock = mapData?.getChildByPath("clock") != null
                map1.everLast = mapData?.getChildByPath("everlast") != null
                map1.town = mapData?.getChildByPath("town") != null
                map1.hpDec = DataTool.getIntConvert("decHP", mapData, 0)
                map1.hpDecProtect = DataTool.getIntConvert("protectItem", mapData, 0)
                map1.forcedReturnMap = DataTool.getInt(mapData?.getChildByPath("info/forcedReturn"), 999999999)
                when (mapId) {
                    190000000, 190000001, 190000002, 191000000, 191000001,
                    192000000, 192000001, 195000000, 195010000, 195020000,
                    195030000, 196000000, 196010000, 197000000, 197010000 -> map1.forcedReturnMap = 193000000
                }
                map1.boat = mapData?.getChildByPath("shipObj") != null
                map1.timeLimit = DataTool.getIntConvert("timeLimit", mapData?.getChildByPath("info"), -1).toLong()
                map1.fieldType = DataTool.getIntConvert("info/fieldType", mapData, 0)
                map1.mobCapacity = (DataTool.getIntConvert("fixedMobCapacity", mapData?.getChildByPath("info"), 500)) //Is there a map that contains more than 500 mobs?
                maps[mapId] = map1
                map = map1
            }
        }
        return map!!
    }

    private fun loadLife(life: Data, id: String?, type: String?): AbstractLoadedLife? {
        val myLife = LifeFactory.getLife(id?.toIntOrNull(), type) ?: return null
        myLife.cy = DataTool.getIntConvert(life.getChildByPath("cy")) ?: 0
        val df = life.getChildByPath("f")
        df?.let { myLife.f = DataTool.getInt((it)) }
        myLife.fh = DataTool.getInt(life.getChildByPath("fh"))
        myLife.rx0 = DataTool.getInt(life.getChildByPath("rx0"))
        myLife.rx1 = DataTool.getInt(life.getChildByPath("rx1"))
        val x = DataTool.getInt(life.getChildByPath("x"))
        val y = DataTool.getInt(life.getChildByPath("y"))
        myLife.position = Point(x, y)
        val hide = DataTool.getInt("hide", life, 0)
        if (hide == 1) {
            myLife.hidden = true
        }
        return myLife
    }

    private fun loadReactor(reactor: Data, id: String): Reactor {
        val x = DataTool.getInt(reactor.getChildByPath("x"))
        val y = DataTool.getInt(reactor.getChildByPath("y"))
        val position = Point(x, y)
        val delay = DataTool.getInt(reactor.getChildByPath("reactorTime")) * 1000
        val state: Byte = 0
        val name = DataTool.getString(reactor.getChildByPath("name"), "")
        return Reactor(ReactorFactory.getReactor(id.toInt()), id.toInt(), delay, state, name, position)
    }

    private fun getMapName(mapId: Int): String {
        val paddedMapId = mapId.toString().padStart(9, '0')
        val area = mapId / 100000000
        return "Map/Map$area/$paddedMapId.img"
    }

    private fun getMapStringName(mapId: Int): String {
        val area = when {
            (mapId < 100000000) -> "maple"
            (mapId in 100000000..199999999) -> "victoria"
            (mapId in 200000000..299999999) -> "ossyria"
            (mapId in 540000000..541010109) -> "singapore"
            (mapId in 600000000..619999999) -> "MasteriaGL"
            (mapId in 670000000..681999999) -> "weddingGL"
            (mapId in 682000000..682999999) -> "HalloweenGL"
            (mapId in 800000000..899999999) -> "jp"
            else -> "etc"
        }
        return "$area/$mapId"
    }

    fun isMapExists(mapId: Int): Boolean {
        val name = getMapName(mapId)
        return source.getData(name) != null
        //return true
    }

    fun isMapLoaded(mapId: Int) = maps.containsKey(mapId)

    companion object : KLogging() {
        val customLife = mutableMapOf<Int, List<AbstractLoadedLife>>()

        private fun loadLife(id: Int, f: Int, hide: Boolean, fh: Int, cy: Int, rx0: Int, rx1: Int, x: Int, y: Int, type: String, mTime: Int): AbstractLoadedLife? {
            val myLife = LifeFactory.getLife(id, type) ?: return null
            myLife.cy = cy
            myLife.f = f
            myLife.fh = fh
            myLife.rx0 = rx0
            myLife.rx1 = rx1
            myLife.position = Point(x, y)
            myLife.hidden = hide
            myLife.mTime = mTime
            myLife.cType = type
            return myLife
        }

        fun loadCustomLife(): Int {
            customLife.clear()
            try {
                val customLifeData = ResourceFile.load("WZCustomLife.json")
                    ?.let { Klaxon().parseArray<WZCustomLifeDatabase>(it) }
                    ?: return -1
                customLifeData.forEach {
                    val mapId = it.mid
                    val myLife = loadLife(
                        it.dataId,
                        it.fh,
                        it.hide > 0,
                        it.fh,
                        it.cy,
                        it.rx0,
                        it.rx1,
                        it.x,
                        it.y,
                        it.type,
                        it.mobTime,
                    ) ?: return@forEach
                    logger.trace { "WZ Custom life: ${myLife.id} is loaded." }
                    val entries = customLife[mapId]
                    val collections = mutableListOf<AbstractLoadedLife>()
                    if (entries != null) collections.addAll(entries)
                    collections.add(myLife)
                    customLife[mapId] = collections
                }
                logger.info { "Successfully loaded ${customLife.size} maps with custom life." }
                return customLife.size
            } catch (e: Exception) {
                logger.error(e) { "Failed to load custom life." }
            }
            return -1
        }
    }
}