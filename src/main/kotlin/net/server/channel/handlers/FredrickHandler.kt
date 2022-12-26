package net.server.channel.handlers

import client.Character
import client.Client
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.ItemFactory
import database.InventoryItems
import mu.KLogging
import net.AbstractPacketHandler
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import server.InventoryManipulator
import server.InventoryManipulator.Companion.addFromDrop
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket
import java.sql.SQLException

class FredrickHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        when (slea.readByte().toInt()) {
            0x1a -> {
                try {
                    val items = ItemFactory.MERCHANT.loadItems(chr.id, false)
                    if (!checkFredrick(chr, items)) {
                        c.announce(CashPacket.fredrickMessage(0x21))
                        return
                    }
                    chr.gainMeso(chr.merchantMeso, false)
                    chr.merchantMeso = 0
                    if (deleteItems(chr)) {
                        for (i in items.indices) {
                            addFromDrop(c, items[i].first, false)
                        }
                        c.announce(CashPacket.fredrickMessage(0x1e))
                    } else {
                        chr.message("알 수 없는 오류가 발생했습니다.")
                    }

                } catch (e: Exception) {
                    logger.error(e) { "Failed to handle Fredrick." }
                }
            }
            else -> return
        }
    }

    companion object : KLogging() {
        private fun checkFredrick(chr: Character, items: List<Pair<Item, InventoryType>>): Boolean {
            if (chr.meso.get() + chr.merchantMeso < 0) return false
            items.forEach { (item, _) ->
                if (!InventoryManipulator.checkSpace(chr.client, item.itemId, item.quantity.toInt(), item.owner)) {
                    return false
                }
            }
            return true
        }

        private fun deleteItems(chr: Character): Boolean {
            try {
                transaction {
                    InventoryItems.deleteWhere {
                        (InventoryItems.type eq ItemFactory.MERCHANT.value.toByte()) and (InventoryItems.characterId eq chr.id)
                    }
                }
                return true
            } catch (e: SQLException) {
                logger.error(e) { "Error caused when delete items in fredrick." }
            }
            return false
        }
    }
}