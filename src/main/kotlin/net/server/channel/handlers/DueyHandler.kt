package net.server.channel.handlers

import client.Character
import client.Client
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.Item
import constants.ItemConstants
import database.Characters
import database.DueyItems
import mu.KLogging
import net.AbstractPacketHandler
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import server.DueyPackages
import server.InventoryManipulator
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import java.sql.SQLException
import java.util.*

class DueyHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.player?.let { player ->
            when (slea.readByte().toInt()) {
                Actions.TOSERVER_SEND_ITEM.code -> {
                    val fee = 5000
                    val inventId = slea.readByte()
                    val itemPos = slea.readShort()
                    val amount = slea.readShort()
                    val mesos = slea.readInt()
                    val recipient = slea.readGameASCIIString()
                    if (mesos < 0 || mesos > Integer.MAX_VALUE || (mesos + fee + getFee(mesos)) > Integer.MAX_VALUE)
                        return
                    val finalCost = mesos + fee + getFee(mesos)
                    var send = false
                    if (player.meso.get() >= finalCost) {
                        val accId = getAccIdFromCname(recipient, true)
                        if (accId != -1) {
                            if (accId != c.accountId) {
                                player.gainMeso(-finalCost, false)
                                c.announce(PacketCreator.sendDuey(Actions.TOCLIENT_SUCCESSFULLY_SENT.code.toByte()))
                                send = true
                            } else {
                                c.announce(PacketCreator.sendDuey(Actions.TOCLIENT_SAMEACC_ERROR.code.toByte()))
                            }
                        } else {
                            c.announce(PacketCreator.sendDuey(Actions.TOCLIENT_NOT_ENOUGH_MESOS.code.toByte()))
                        }
                        var recipientOn = false
                        var client: Client? = null
                        val channel = c.getWorldServer().findChannelIdByCharacterName(recipient)
                        if (channel > -1) {
                            recipientOn = true
                            val ch = c.getWorldServer().getChannel(channel)
                            client = ch.players.getCharacterByName(recipient)?.client
                        }
                        if (send) {
                            if (inventId > 0) {
                                val inv = InventoryType.getByType(inventId)
                                val item = player.getInventory(inv)?.getItem(itemPos.toByte())
                                if (item != null && player.getItemQuantity(item.itemId, false) > amount) {
                                    if (ItemConstants.isRechargeable(item.itemId)) {
                                        InventoryManipulator.removeFromSlot(
                                            c,
                                            inv,
                                            itemPos.toByte(),
                                            item.quantity,
                                            fromDrop = true,
                                            consume = false
                                        )
                                    } else {
                                        InventoryManipulator.removeFromSlot(
                                            c,
                                            inv,
                                            itemPos.toByte(),
                                            amount,
                                            fromDrop = true,
                                            consume = false
                                        )
                                    }
                                    addItemToDatabase(
                                        item,
                                        amount.toInt(),
                                        mesos,
                                        player.name,
                                        getAccIdFromCname(recipient, false)
                                    )
                                } else return
                            } else {
                                addMesoToDatabase(mesos, player.name, getAccIdFromCname(recipient, false))
                            }
                            if (recipientOn && client != null) {
                                client.announce(PacketCreator.sendDuey(Actions.TOCLIENT_PACKAGE_MSG.code.toByte()))
                            }
                        }
                    }
                }

                Actions.TOSERVER_CLAIM_PACKAGE.code -> {
                    val packageId = slea.readInt()
                    val packages = mutableListOf<DueyPackages>()
                    val dp: DueyPackages? = null
                    try {
                        var dueyPack: DueyPackages
                        transaction {
                            val row =
                                (database.DueyPackages leftJoin DueyItems).select(
                                    database.DueyPackages.senderName,
                                    database.DueyPackages.mesos,
                                    database.DueyPackages.timestamp
                                ).where { database.DueyPackages.packageId eq packageId }
                            if (!row.empty()) {
                                val rs = row.first()
                                dueyPack = getItemByPid(rs) ?: return@transaction
                                dueyPack.sender = rs[database.DueyPackages.senderName]
                                dueyPack.mesos = rs[database.DueyPackages.mesos]
                                dueyPack.setSentTime(rs[database.DueyPackages.timestamp])
                                packages.add(dueyPack)
                            }
                        }
                    } catch (e: SQLException) {
                        logger.error(e) { "Failed to get duey package to database. PackageId: $packageId" }
                    }
                    if (dp?.item != null) {
                        if (!InventoryManipulator.checkSpace(
                                c,
                                dp.item.itemId,
                                dp.item.quantity.toInt(),
                                dp.item.owner
                            )
                        ) {
                            player.dropMessage(1, "인벤토리가 꽉 찼습니다.")
                            c.announce(PacketCreator.enableActions())
                            return
                        } else {
                            InventoryManipulator.addFromDrop(c, dp.item, false)
                        }
                    }
                    val gainMesos = 0
                    val totalMesos = (dp?.mesos ?: 0) + player.meso.get()
                    if (totalMesos < 0 || (dp?.mesos ?: 0) < 0) {
                        player.gainMeso(player.meso.get(), false)
                    } else {
                        player.gainMeso(gainMesos, false)
                    }
                    removeItemFromDatabase(packageId)
                    //       c.announce(JPacketCreator.removeItemFromDuey(false, packageId))
                }

                else -> {}
            }
        }
    }

    private fun addMesoToDatabase(mesos: Int, name: String, recipientId: Int) =
        addItemToDatabase(null, 1, mesos, name, recipientId)

    private fun addItemToDatabase(item: Item?, quantity: Int, mesos: Int, name: String, recipientId: Int) {
        try {
            transaction {
                val row = database.DueyPackages.insert {
                    it[receiverId] = recipientId
                    it[senderName] = name
                    it[database.DueyPackages.mesos] = mesos
                    it[timestamp] = getCurrentDate()
                    it[checked] = true
                    if (item == null) {
                        it[type] = 3
                    } else {
                        it[type] = item.getType().toByte()
                    }
                }
                if (item != null) {
                    val rs = row.resultedValues?.first() ?: return@transaction
                    DueyItems.insert {
                        if (item.getType() == 1) {
                            item as Equip
                            it[itemId] = item.itemId
                            it[DueyItems.quantity] = 1
                            it[upgradeSlots] = item.upgradeSlots.toInt()
                            it[level] = item.level.toInt()
                            it[str] = item.str.toInt()
                            it[dex] = item.dex.toInt()
                            it[int] = item.int.toInt()
                            it[luk] = item.luk.toInt()
                            it[hp] = item.hp.toInt()
                            it[mp] = item.mp.toInt()
                            it[watk] = item.watk.toInt()
                            it[matk] = item.matk.toInt()
                            it[wdef] = item.wdef.toInt()
                            it[mdef] = item.mdef.toInt()
                            it[acc] = item.acc.toInt()
                            it[avoid] = item.avoid.toInt()
                            it[hands] = item.hands.toInt()
                            it[speed] = item.speed.toInt()
                            it[jump] = item.jump.toInt()
                            it[owner] = item.owner
                        } else {
                            it[itemId] = item.itemId
                            it[DueyItems.quantity] = quantity
                            it[owner] = item.owner
                        }
                        it[packageId] = rs[packageId]
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to save duey item to database. SenderName: $name, ItemId: ${item?.itemId}" }
        }
    }

    private fun getCurrentDate(): String {
        var date = ""
        val cal = Calendar.getInstance()
        val day = cal[Calendar.DATE] - 1 // instant duey ?
        val month = cal[Calendar.MONTH] + 1 // its an array of months.
        val year = cal[Calendar.YEAR]
        date += if (day < 9) "0" else "$day-"
        date += if (month < 9) "0" else "$month-"
        date += year
        return date
    }

    private fun removeItemFromDatabase(packageId: Int) {
        try {
            transaction {
                database.DueyPackages.deleteWhere { database.DueyPackages.packageId eq packageId }
                DueyItems.deleteWhere { DueyItems.packageId eq packageId }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to remove duey item from database. PackageId: $packageId" }
        }
    }

    private enum class Actions(val code: Int) {
        TOSERVER_SEND_ITEM(0x02),
        TOSERVER_CLAIM_PACKAGE(0x04),
        TOSERVER_REMOVE_PACKAGE(0x05),
        TOSERVER_CLOSE_DUEY(0x07),
        TOCLIENT_OPEN_DUEY(0x08),
        TOCLIENT_NOT_ENOUGH_MESOS(0x0A),
        TOCLIENT_NAME_DOES_NOT_EXIST(0x0C),
        TOCLIENT_SAMEACC_ERROR(0x0D),
        TOCLIENT_SUCCESSFULLY_SENT(0x12),
        TOCLIENT_SUCCESSFUL_MSG(0x17),
        TOCLIENT_PACKAGE_MSG(0x1B); // Ending byte; 4 if recieved. 3 if delete.
    }

    companion object : KLogging() {
        fun getAccIdFromCname(name: String, accountId: Boolean): Int {
            var result = -1
            try {
                transaction {
                    val row = Characters.select(Characters.accountId, Characters.id).where { Characters.name eq name }
                    if (row.empty()) return@transaction
                    val it = row.first()
                    result = if (accountId) it[Characters.accountId] else it[Characters.id]
                }
            } catch (e: Exception) {
                logger.error(e) { "Error caused when try get account id using character name. Query: $name" }
            }
            return result
        }

        fun getFee(meso: Int): Int {
            return when {
                meso >= 10000000 -> meso / 25
                meso >= 5000000 -> meso * 3 / 100
                meso >= 1000000 -> meso / 50
                meso >= 100000 -> meso / 100
                meso >= 50000 -> meso / 200
                else -> 0
            }
        }

        fun getItemByPid(rs: ResultRow?): DueyPackages? {
            if (rs == null) return null
            try {
                var dueyPack: DueyPackages? = null
                transaction {
                    if (rs[database.DueyPackages.type].toInt() == 1) {
                        val eq = Equip(rs[DueyItems.itemId], 0, -1)
                        eq.upgradeSlots = rs[DueyItems.upgradeSlots].toByte()
                        eq.level = rs[DueyItems.level].toByte()
                        eq.str = rs[DueyItems.str].toShort()
                        eq.dex = rs[DueyItems.dex].toShort()
                        eq.int = rs[DueyItems.int].toShort()
                        eq.luk = rs[DueyItems.luk].toShort()
                        eq.hp = rs[DueyItems.hp].toShort()
                        eq.mp = rs[DueyItems.mp].toShort()
                        eq.watk = rs[DueyItems.watk].toShort()
                        eq.matk = rs[DueyItems.matk].toShort()
                        eq.wdef = rs[DueyItems.wdef].toShort()
                        eq.mdef = rs[DueyItems.mdef].toShort()
                        eq.acc = rs[DueyItems.acc].toShort()
                        eq.avoid = rs[DueyItems.avoid].toShort()
                        eq.hands = rs[DueyItems.hands].toShort()
                        eq.speed = rs[DueyItems.speed].toShort()
                        eq.jump = rs[DueyItems.jump].toShort()
                        eq.owner = rs[DueyItems.owner]
                        dueyPack = DueyPackages(rs[DueyItems.packageId], eq)
                    } else if (rs[database.DueyPackages.type] == 2.toByte()) {
                        val newItem = Item(rs[DueyItems.itemId], 0, rs[DueyItems.quantity].toShort())
                        newItem.owner = rs[DueyItems.owner]
                        dueyPack = DueyPackages(rs[DueyItems.packageId], newItem)
                    } else {
                        dueyPack = DueyPackages(rs[DueyItems.packageId], null)
                    }
                }
                return dueyPack
            } catch (e: SQLException) {
                logger.error(e) { "Failed to get duey item from package id." }
            }
            return null
        }

        fun loadItems(chr: Character?): List<DueyPackages> {
            if (chr == null) return emptyList()
            val packages = mutableListOf<DueyPackages>()
            try {
                transaction {
                    (database.DueyPackages leftJoin DueyItems).select(
                        database.DueyPackages.senderName,
                        database.DueyPackages.mesos,
                        database.DueyPackages.timestamp
                    ).where { database.DueyPackages.receiverId eq chr.id }.forEach {
                        val dueyPack = getItemByPid(it) ?: return@forEach
                        dueyPack.sender = it[database.DueyPackages.senderName]
                        dueyPack.mesos = it[database.DueyPackages.mesos]
                        dueyPack.setSentTime(it[database.DueyPackages.timestamp])
                        packages.add(dueyPack)
                    }
                }
                return packages
            } catch (e: SQLException) {
                logger.error(e) { "Failed to load duey items. CharacterId: ${chr.id}" }
            }
            return emptyList()
        }
    }
}