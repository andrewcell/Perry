package server.maps

import provider.DataProvider
import provider.DataProviderFactory
import provider.DataTool
import java.io.File
import tools.ServerJSON.settings

class ReactorFactory {
    companion object {
        val data: DataProvider = DataProviderFactory.getDataProvider(File("${settings.wzPath}/Reactor.wz"))
        private val reactorStats = mutableMapOf<Int, ReactorStats>()

        fun getReactor(rid: Int): ReactorStats {
            var stats = reactorStats[rid]
            if (stats == null) {
                var infoId = rid
                var reactorData = data.getData("$infoId.img".padStart(11, '0'))
                val link = reactorData?.getChildByPath("info/link")
                if (link != null) {
                    infoId = DataTool.getIntConvert("info/link", reactorData)
                    stats = reactorStats[infoId]
                }
                val activateOnTouch = reactorData?.getChildByPath("info/activateByTouch")
                val loadArea = if (activateOnTouch != null) DataTool.getInt("info/activateByTouch", reactorData, 0) != 0 else false
                if (stats == null) {
                    reactorData = data.getData("$infoId.img".padStart(11, '0'))
                    var reactorInfoData = reactorData?.getChildByPath("0")
                    stats = ReactorStats()
                    var stateDataes = mutableListOf<ReactorStats.Companion.StateData>()
                    if (reactorInfoData != null) {
                        var areaSet = false
                        var i: Byte = 0
                        while (reactorInfoData != null) {
                            val eventData = reactorInfoData.getChildByPath("event")
                            if (eventData != null) {
                                eventData.children.forEach {
                                    if (it.name == "timeOut") return@forEach
                                    var reactItem: Pair<Int, Int>? = null
                                    val type = DataTool.getIntConvert("type", it)
                                    if (type == 100) { // Reactor waits for item
                                        reactItem = Pair(DataTool.getIntConvert("0", it), DataTool.getIntConvert("1", it))
                                        if (!areaSet || loadArea) {
                                            stats.tl = DataTool.getPoint("lt", it)
                                            stats.br = DataTool.getPoint("rb", it)
                                            areaSet = false
                                        }
                                    }
                                    val activeSkillId = it.getChildByPath("activeSkillID")
                                    val skillIds = mutableListOf<Int>()
                                    activeSkillId?.forEach { skill -> skillIds.add(DataTool.getInt(skill)) }
                                    val nextState = DataTool.getIntConvert("state", it).toByte()
                                    stateDataes.add(
                                        ReactorStats.Companion.StateData(
                                            type,
                                            reactItem,
                                            skillIds,
                                            nextState
                                        )
                                    )
                                }
                                stats.addState(i, stateDataes)
                            }
                            i++
                            reactorInfoData = reactorData?.getChildByPath(i.toString())
                            stateDataes = mutableListOf()
                        }

                    } else {
                        stateDataes.add(ReactorStats.Companion.StateData(999, null, null, 0))
                        stats.addState(0, stateDataes)
                    }
                    reactorStats[infoId] = stats
                    if (rid != infoId) {
                        reactorStats[rid] = stats
                    }
                } else {
                    reactorStats[rid] = stats
                }
            }
            return stats
        }
    }
}