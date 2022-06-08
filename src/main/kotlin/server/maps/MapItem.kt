package server.maps

import client.Character
import client.Client
import client.inventory.Item
import tools.packet.GameplayPacket
import tools.packet.ItemPacket
import java.awt.Point
import java.util.concurrent.locks.ReentrantLock

class MapItem(val meso: Int = 0, val item: Item? = null, pos: Point, val dropper: MapObject, owner: Character?, val dropType: Byte, val playerDrop: Boolean, val questId: Int = -1) : AbstractMapObject(){
    override val objectType: MapObjectType = MapObjectType.ITEM
    override var position: Point = pos
    val ownerId = if (questId == -1 || meso <= 0) owner?.id else (if (owner?.party == null) owner?.id else owner.getPartyId())
    var pickedUp = false
    val itemLock = ReentrantLock()

    fun getItemId() = if (meso > 0) meso else item?.itemId ?: meso

    override fun sendSpawnData(client: Client) {
        client.player?.let { p ->
            if (item != null && (questId <= 0 || (p.getQuestStatus(questId) == 1 && p.needQuestItem(questId, item.itemId)))) {
                client.announce(ItemPacket.dropItemFromMapObject(this, null, position, 2))
            }
        }
    }

    override fun sendDestroyData(client: Client) {
        client.announce(GameplayPacket.removeItemFromMap(objectId, 1, 0))
    }

}