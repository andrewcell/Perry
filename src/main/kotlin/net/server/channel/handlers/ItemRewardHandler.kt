package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import constants.ItemConstants.Companion.getInventoryType
import net.AbstractPacketHandler
import net.server.Server.broadcastMessage
import server.InventoryManipulator.Companion.addById
import server.InventoryManipulator.Companion.addFromDrop
import server.InventoryManipulator.Companion.checkSpace
import server.InventoryManipulator.Companion.removeById
import server.ItemInformationProvider
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import tools.packet.InteractPacket
import kotlin.random.Random

class ItemRewardHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val slot = slea.readShort().toByte()
        val itemId = slea.readInt() // will load from xml I don't care.
        if (c.player?.getInventory(InventoryType.USE)?.getItem(slot)?.itemId != itemId ||
            (c.player?.getInventory(InventoryType.USE)?.countById(itemId) ?: 0) < 1
        ) return
        val (first, second) = ItemInformationProvider.getItemReward(itemId)
        for (reward in second) {
            if (!checkSpace(c, reward.itemId, reward.quantity.toInt(), "")) {
                c.announce(CharacterPacket.getShowInventoryFull())
                break
            }
            if (Random.nextInt(first) < reward.prob) { //Is it even possible to get an item with prob 1?
                if (getInventoryType(reward.itemId) === InventoryType.EQUIP) {
                    val item = ItemInformationProvider.getEquipById(reward.itemId)
                    if (reward.period != -1) {
                        item.expiration = System.currentTimeMillis() + reward.period * 60 * 60 * 10
                    }
                    addFromDrop(c, item, false)
                } else {
                    addById(c, reward.itemId, reward.quantity)
                }
                removeById(c, InventoryType.USE, itemId, 1, fromDrop = false, consume = false)
                if (reward.worldMessage != null) {
                    val msg = reward.worldMessage ?: ""
                    msg.replace("/name".toRegex(), c.player?.name ?: "")
                    msg.replace("/item".toRegex(), ItemInformationProvider.getName(reward.itemId) ?: "")
                    broadcastMessage(c.world, InteractPacket.serverNotice(6, msg))
                }
                break
            }
        }
        c.announce(PacketCreator.enableActions())
    }
}