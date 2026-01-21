package net.server.channel.handlers

import client.Client
import database.NXCodes
import mu.KLoggable
import net.AbstractPacketHandler
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import server.InventoryManipulator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket
import java.sql.SQLException

/**
 * Coupon code type.
 * 1 : NX Credit
 * 2 : M. Point
 * 4 : Actual item
 */
class CouponCodeHandler : AbstractPacketHandler(), KLoggable {
    override val logger = logger()

    fun getNXCode(code: String, type: String): Int {
        var item = -1
        try {
            transaction {
                NXCodes.selectAll().where { NXCodes.code eq code }.forEach {
                    item = it[NXCodes.type]
                }
            }
        } catch (e: SQLException) {
            logger.warn(e) { "Failed to get nx code from database. Code: $code" }
        }
        return item
    }

    fun getNXCodeValid(code: String): Boolean {
        var valid = false
        try {
            transaction {
                NXCodes.selectAll().where { NXCodes.code eq code }.forEach {
                    valid = it[NXCodes.valid] != 0
                }
            }
        } catch (e: SQLException) {
            logger.warn(e) { "Failed to get nx code from database. Code: $code" }
        }
        return valid
    }

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        slea.skip(2)
        val code = slea.readGameASCIIString()
        var validCode = false
        val type: Int
        val item: Int
        validCode = getNXCodeValid(code.uppercase())
        if (validCode) {
            type = getNXCode(code, "type")
            item = getNXCode(code, "item")
            if (type != 5) {
                try {
                    transaction {
                        NXCodes.update({ NXCodes.code eq code }) {
                            it[valid] = 0
                            it[user] = c.player?.name.toString()
                        }
                    }
                } catch (e: SQLException) {
                    logger.warn(e) { "Failed to update used nx code to database. Code: $code" }
                }
            }
            when (type) {
                0, 1, 2 -> {
                    c.player?.cashShop?.gainCash(type, item)
                }
                3 -> {
                    c.player?.cashShop?.gainCash(0, item)
                    c.player?.cashShop?.gainCash(2, item / 5000)
                }
                4 -> {
                    InventoryManipulator.addById(c, item, 1, null, -1, -1)
                    c.announce(CashPacket.showCouponRedeemedItem(item))
                }
                5 -> {
                    c.player?.cashShop?.gainCash(0, item)
                }
                else -> {}
            }
            c.player?.let { c.announce(CashPacket.showCash(it)) }
        }
        c.announce(CashPacket.enableCSUse())
    }
}