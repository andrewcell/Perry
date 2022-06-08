package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import server.InventoryManipulator
import server.maps.MapItem
import server.maps.MapObjectType
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import tools.packet.GameplayPacket
import tools.packet.ItemPacket

class PetLootHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        val pet = chr.pet
        if (pet?.summoned == false) return
        slea.skip(4)
        val oid = slea.readInt()
        val ob = try {
            chr.map.mapObjects[oid]
        } catch (e: Exception) {
            c.announce(CharacterPacket.getInventoryFull())
            return
        }
        if (ob == null || pet == null || ob.objectType != MapObjectType.ITEM) {
            c.announce(CharacterPacket.getInventoryFull())
            return
        }
        ob as MapItem
        if (ob.pickedUp) {
            c.announce(CharacterPacket.getInventoryFull())
            return
        }
        if (ob.meso > 0) {
            if (chr.party != null && ob.ownerId != chr.id) {
                val mesoAmm = ob.meso
                if (mesoAmm > 50000 * chr.mesoRate) return
                if (!ob.playerDrop) {
                    var partyNum = 0
                    chr.party?.members?.forEach {
                        if (it.online && it.mapId == chr.map.mapId && it.channel == c.channel) {
                            partyNum
                        }
                    }
                    chr.party?.members?.forEach {
                        if (it.online && it.mapId == chr.map.mapId) {
                            val someChr = c.getChannelServer().players.getCharacterById((it.id ?: 0))
                            someChr?.gainMeso(mesoAmm / partyNum, show = true, enableActions = false, inChat = false)
                        }
                    }
                } else {
                    chr.gainMeso(ob.meso, show = true, enableActions = false, inChat = false)
                }
            } else {
                chr.gainMeso(ob.meso, show = true, enableActions = false, inChat = false)
            }
            ob.pickedUp = true
            chr.map.broadcastMessage(GameplayPacket.removeItemFromMap(ob.objectId, 5, chr.id, true, 0), ob.position)
            chr.map.removeMapObject(ob)
        } else {
            if (ob.questId > 0 && !chr.needQuestItem(ob.questId, ob.getItemId())) {
                c.announce(ItemPacket.showItemUnavailable())
                return
            }
            if (ob.item?.let { InventoryManipulator.addFromDrop(c, it, true) } == true) {
                ob.pickedUp = true
                chr.map.broadcastMessage(GameplayPacket.removeItemFromMap(ob.objectId, 5, chr.id, true, 0), ob.position)
                chr.map.removeMapObject(ob)
                c.announce(CharacterPacket.getInventoryFull())
            }
        }
    }
}