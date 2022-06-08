package server

import client.Client
import java.awt.Point

interface Portal {
    val type: Int
    var id: Int
    var position: Point?
    var name: String?
    var target: String?
    var scriptName: String?
    var portalStatus: Boolean
    var targetMapId: Int
    fun enterPortal(c: Client)
    var portalState: Boolean

    companion object {
        const val open = true
        //const val closed = false
        const val mapPortal = 2
        const val doorPortal = 6
    }
}