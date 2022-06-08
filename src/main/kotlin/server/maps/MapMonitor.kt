package server.maps

import server.Portal
import tools.CoroutineManager

class MapMonitor(val map: GameMap, val portal: String) {
    private val monitorSchedule = CoroutineManager.register({
        if (map.characters.isEmpty()) cancelAction()
    }, 5000, 0)
    private val portalObject = map.getPortal(portal)

    private fun cancelAction() {
        monitorSchedule.cancel()
        map.killAllMonsters()
        map.clearDrops()
        portalObject?.portalStatus = Portal.open
        map.resetReactors()
    }
}