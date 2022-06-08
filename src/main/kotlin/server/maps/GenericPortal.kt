package server.maps

import client.Client
import scripting.portal.PortalScriptManager
import server.Portal
import tools.PacketCreator
import java.awt.Point

open class GenericPortal(override val type: Int) : Portal {
    override var id: Int = -1
    override var position: Point? = null
    override var name: String? = null
    override var target: String? = null
    override var scriptName: String? = null
    override var portalStatus: Boolean = false
    override var targetMapId: Int = 999999999
    override var portalState: Boolean = false

    override fun enterPortal(c: Client) {
        var changed = false
        if (scriptName == null || scriptName == "" || targetMapId != 999999999) {
            val to = if (c.player?.eventInstance == null) c.getChannelServer().mapFactory.getMap(targetMapId) else c.player?.eventInstance?.getMapInstance(targetMapId)
            val pto = target?.let { to?.getPortal(it) } ?: to?.getPortal(0)
            to?.let { c.player?.changeMap(it, pto) }
            changed = true
        } else {
            changed = PortalScriptManager.executePortalScript(this, c)
        }
        if (!changed) {
            c.announce(PacketCreator.enableActions())
        }
    }
}