package scripting.portal

interface PortalScript {
    fun enter(ppi: PortalPlayerInteraction): Boolean
}