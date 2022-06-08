package server.maps

import client.Character
import client.Client
import server.Portal
import tools.PacketCreator
import tools.packet.GameplayPacket
import tools.packet.InteractPacket
import java.awt.Point

class Door(val owner: Character, val targetPosition: Point) : AbstractMapObject() {
    val target: GameMap = owner.map
    val town = target.getReturnMap()
    val townPortal = getFreePortal()
    override var position = targetPosition

    private fun getFreePortal(): Portal {
        val freePortals = mutableListOf<Portal>()
        town.portals.values.forEach { p ->
            if (p.type == 6) freePortals.add(p)
        }
        freePortals.sortWith { o1: Portal, o2: Portal ->
            if (o1.id < o2.id) {
                return@sortWith -1
            } else if (o1.id == o2.id) {
                return@sortWith 0
            } else {
                return@sortWith 1
            }
        }
        town.mapObjects.values.forEach { o ->
            if (o is Door) {
                if (o.owner.party != null && o.owner.mpc?.let { owner.party!!.containsMembers(it) } == true) {
                    freePortals.remove(o.townPortal)
                }
            }
        }
        return freePortals.iterator().next()
    }

    fun warp(chr: Character, toTown: Boolean) {
        if (chr == owner || owner.party != null && chr.mpc?.let { owner.party!!.containsMembers(it) } == true) {
            if (!toTown) chr.changeMap(target, targetPosition) else chr.changeMap(town, townPortal)
        } else {
            chr.client.announce(PacketCreator.enableActions())
        }
    }

    override val objectType = MapObjectType.DOOR

    override fun sendSpawnData(client: Client) {
        client.player?.let { player ->
            if (target.mapId == player.mapId || owner == client.player && owner.party == null) {
                client.announce(GameplayPacket.spawnDoor(owner.id, if (town.mapId == player.mapId) townPortal.position else targetPosition, true))
                if (owner.party != null && (owner == client.player || owner.party!!.containsMembers(player.mpc))) {
                    client.announce(InteractPacket.partyPortal(town.mapId, target.mapId, targetPosition))
                }
                client.announce(GameplayPacket.spawnPortal(town.mapId, target.mapId, targetPosition))
            }
        }
    }

    override fun sendDestroyData(client: Client) {
        client.player?.let { player ->
            if (target.mapId == player.mapId || owner === client.player || owner.party != null && owner.party!!.containsMembers(player.mpc)) {
                if (owner.party != null && (owner === client.player || owner.party!!.containsMembers(player.mpc))) {
                    client.announce(InteractPacket.partyPortal(999999999, 999999999, Point(-1, -1)))
                }
                client.announce(GameplayPacket.removeDoor(owner.id, false))
                client.announce(GameplayPacket.removeDoor(owner.id, true))
            }
        }
    }

}