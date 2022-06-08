package server.life

import client.Client
import server.ShopFactory
import server.maps.AbstractLoadedLife
import server.maps.MapObjectType
import tools.packet.NpcPacket

class Npc(id: Int, val stats: NpcStats) : AbstractLoadedLife(id) {
    fun hasShop() = ShopFactory.getShopForNpc(id) != null

    fun sendShop(c: Client) = ShopFactory.getShopForNpc(id)?.sendShop(c)

    override fun sendSpawnData(client: Client) {
        if (!this.hidden) {
            if (id in 9010011..9010013) {
                client.announce(NpcPacket.spawnNpcRequestController(this))
            } else {
                client.announce(NpcPacket.spawnNpc(this))
                client.announce(NpcPacket.spawnNpcRequestController(this))
            }
        }
    }

    override fun sendDestroyData(client: Client) = client.announce(NpcPacket.removeNpc(objectId))

    override val objectType = MapObjectType.NPC
}