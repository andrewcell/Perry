package net.server.channel.handlers

import client.Client
import client.autoban.AutobanFactory
import mu.KLogging
import net.AbstractPacketHandler
import server.InventoryManipulator.Companion.addFromDrop
import server.maps.MapItem
import server.maps.MapObjectType
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import tools.packet.GameplayPacket
import tools.packet.ItemPacket

class ItemPickupHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        //slea.readInt(); //Timestamp
        //slea.readByte();
        val cPos = slea.readPos()
        val oid = slea.readInt()
        val chr = c.player ?: return
        val obj = chr.map.mapObjects[oid]
        if (obj == null || obj.objectType != MapObjectType.ITEM) {
            c.announce(PacketCreator.enableActions())
            return
        }
        obj as MapItem
        if (obj.pickedUp) {
            c.announce(PacketCreator.enableActions())
            return
        }
        val distance = cPos.distanceSq(obj.position)
        if (distance > 15000) {
            //    AutoBanFactory.SHORT_ITEM_VAC.autoBan(chr, cPos.toString() + Distance);
            c.disconnect(shutdown = false, cashShop = false)
            println(chr.name + ":: SHORT_ITEM_VAC ::" + cPos.toString() + distance)
        } else if (chr.position.distanceSq(obj.position) > 80000.0) {
            AutobanFactory.ITEM_VAC.autoban(chr, cPos.toString() + distance)
        }
        if (obj.meso > 0) {
            if (chr.party != null && obj.ownerId != chr.id) {
                val mesosAmm = obj.meso
                if (mesosAmm > 50000 * chr.mesoRate) {
                    return
                }
                if (!obj.playerDrop) {
                    var partyNum = 0
                    chr.party?.members?.forEach { partyMem ->
                        if (partyMem.online && partyMem.mapId == chr.map.mapId && partyMem.channel == c.channel) {
                            partyNum++
                        }
                    }
                    chr.party?.members?.forEach { partyMem ->
                        if (partyMem.online && partyMem.mapId == chr.map.mapId) {
                            val someCharacter = partyMem.id?.let { c.getChannelServer().players.getCharacterById(it) }
                            if (someCharacter != null) {
                                someCharacter.gainMeso(mesosAmm / partyNum,
                                    show = true,
                                    enableActions = false,
                                    inChat = false
                                )
                                c.announce(PacketCreator.enableActions())
                            }
                        }
                    }
                } else {
                    chr.gainMeso(obj.meso, show = true, enableActions = true, inChat = false)
                }
            } else {
                chr.gainMeso(obj.meso, show = true, enableActions = true, inChat = false)
            }
            obj.pickedUp = true
            chr.map.broadcastMessage(
                GameplayPacket.removeItemFromMap(obj.objectId, 2, chr.id),
                obj.position
            )
            chr.map.removeMapObject(obj)
        } else {
            if (obj.questId > 0 && !chr.needQuestItem(obj.questId, obj.getItemId())) {
                c.announce(ItemPacket.showItemUnavailable())
                c.announce(PacketCreator.enableActions())
                return
            }
            obj.item?.let {
                if (addFromDrop(c, obj.item, true)) {
                    obj.pickedUp = true
                    chr.map.broadcastMessage(
                        GameplayPacket.removeItemFromMap(obj.objectId, 2, chr.id),
                        obj.position
                    )
                    chr.map.removeMapObject(obj)
                    c.announce(CharacterPacket.getInventoryFull())
                    c.announce(PacketCreator.enableActions())
                }
            }
        }
    }

    companion object : KLogging()
}