package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import constants.ExpTable
import net.AbstractPacketHandler
import server.InventoryManipulator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import kotlin.math.max

class UseMountFoodHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        slea.skip(6)
        val itemId = slea.readInt()
        if (c.player?.getInventory(InventoryType.USE)?.findById(itemId) != null) {
            c.player?.let { player ->
                val mount = player.mount
                if (mount != null && mount.tiredness > 0) {
                    player.mount?.tiredness = max(mount.tiredness - 30, 0)
                    player.mount?.exp = 2 * mount.level + 6 + mount.exp
                    val level = mount.level
                    val levelUp = mount.exp >= ExpTable.getMountExpNeededForLevel(level) && level < 31
                    if (levelUp) mount.level = level + 1
                    player.map.broadcastMessage(CharacterPacket.updateMount(player.id, player.mount, levelUp))
                    InventoryManipulator.removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = false)
                }
            }
        }
    }
}