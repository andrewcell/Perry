package server

import provider.Data
import provider.DataTool
import server.maps.GenericPortal
import server.maps.MapPortal
import java.awt.Point

class PortalFactory {
    private var nextDoorPortal = 0x80

    fun makePortal(type: Int, portal: Data): Portal {
        val ret: Portal = if (type == Portal.mapPortal) MapPortal() else GenericPortal(type)
        loadPortal(ret, portal)
        return ret
    }

    private fun loadPortal(myPortal: Portal, portal: Data) {
        myPortal.name = DataTool.getString((portal.getChildByPath("pn")))
        myPortal.target = DataTool.getString((portal.getChildByPath("tn")))
        myPortal.targetMapId = DataTool.getInt((portal.getChildByPath("tm")))
        val x = DataTool.getInt((portal.getChildByPath("x")))
        val y = DataTool.getInt((portal.getChildByPath("y")))
        myPortal.position = Point(x,y)
        val script = DataTool.getStringNullable("script", portal, null)
        myPortal.scriptName = script
        myPortal.portalStatus = true
        if (myPortal.type == Portal.doorPortal) {
            myPortal.id = nextDoorPortal
            nextDoorPortal++
        } else {
            myPortal.id = portal.name.toInt()
        }
    }
}