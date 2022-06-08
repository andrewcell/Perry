package server.maps

import client.Client
import database.PlayerNpcsEquip
import mu.KLoggable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import tools.PacketCreator
import tools.packet.GameplayPacket
import java.awt.Point
import java.sql.SQLException

class PlayerNPCs(
    val rowId: Int,
    val npcId: Int,
    val name: String,
    val cy: Int,
    val hair: Int,
    val face: Int,
    val skin: Byte,
    val fh: Int,
    val rx0: Int,
    val rx1: Int,
    x: Int
) : AbstractMapObject(), KLoggable {
    override val logger = logger()
    val equips = mutableMapOf<Byte, Int>()

    init {
        position = Point(x, cy)
        try {
            transaction {
                PlayerNpcsEquip.slice(PlayerNpcsEquip.equipPos, PlayerNpcsEquip.equipId).select {
                    PlayerNpcsEquip.npcId eq rowId
                }.forEach {
                    equips[it[PlayerNpcsEquip.equipPos].toByte()] = it[PlayerNpcsEquip.equipId]
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to init player NPCs." }
        }
    }

    override val objectType = MapObjectType.PLAYER_NPC

    override fun sendDestroyData(client: Client) {
        return
    }

    override fun sendSpawnData(client: Client) {
        client.announce(GameplayPacket.spawnPlayerNpc(this))
        client.announce(PacketCreator.getPlayerNpc(this))
    }
}