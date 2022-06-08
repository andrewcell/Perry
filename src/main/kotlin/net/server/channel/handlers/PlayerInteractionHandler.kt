package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import constants.ItemConstants
import net.AbstractPacketHandler
import server.*
import server.InventoryManipulator.Companion.addFromDrop
import server.InventoryManipulator.Companion.removeFromSlot
import server.maps.FieldLimit
import server.maps.HiredMerchant
import server.maps.MapObjectType
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket
import tools.packet.CharacterPacket
import tools.packet.InteractPacket
import tools.packet.MiniGamePacket

class PlayerInteractionHandler : AbstractPacketHandler() {
    enum class Action(val code: Int) {
        CREATE(0),
        INVITE(2),
        DECLINE(3),
        VISIT(4),
        ROOM(5),
        CHAT(6),
        CHAT_THING(8),
        EXIT(0xA),
        OPEN(0xB),
        TRADE_BIRTHDAY(0x0C),
        SET_ITEMS(0x0D),
        SET_MESO(0x0E),
        CONFIRM(0x0F),
        TRANSACTION(0x10), // ??
        ADD_ITEM(0x12),
        BUY(0x13),
        UPDATE_MERCHANT(0x15),
        REMOVE_ITEM(0x16), // ??
        BAN_PLAYER(0x18),
        MERCHANT_THING(0x19),
        OPEN_STORE(0x1A),
        PUT_ITEM(0x1D),
        MERCHANT_BUY(0x1E),
        TAKE_ITEM_BACK(0x22),
        MAINTENANCE_OFF(0x23),
        MERCHANT_ORGANIZE(0x24),
        CLOSE_MERCHANT(0x25),
        REAL_CLOSE_MERCHANT(0xFF),
        MERCHANT_MESO(0x27),
        SOMETHING(0x29),
        VIEW_VISITORS(0xFE),
        BLACKLIST(0xFC),
        REQUEST_TIE(0x2A),
        ANSWER_TIE(0x2B),
        GIVE_UP(0x2C),
        REQUEST_REDO(0x2E),
        ANSWER_REDO(0x2F),
        EXIT_AFTER_GAME(0x30),
        CANCEL_EXIT(0x31),
        READY(0x32),
        UN_READY(0x33),
        MINIGAME_BAN_PLAYER(0x34),
        START(0x35),
        GET_RESULT(0x36),
        SKIP(0x37),
        MOVE_OMOK(0x38),
        SELECT_CARD(0x3C);

        companion object {
            fun findByCode(code: Int) = values().find { it.code == code }
        }
    }

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val mode = slea.readByte()
        val chr = c.player
        if (chr != null) {
            when (Action.findByCode(mode.toInt())) {
                Action.CREATE -> {
                    when (val createType = slea.readByte().toInt()) {
                        3 -> { // Trade
                            Trade.startTrade(chr)
                        }
                        1 -> { // Omok mini game
                            if (FieldLimit.CANNOTMINIGAME.check(chr.map.fieldLimit)) return
                            val desc = slea.readGameASCIIString()
                            val locker = slea.readByte().toInt()
                            val password = if (locker == 1) slea.readGameASCIIString() else null
                            val type = slea.readByte().toInt()
                            val game = MiniGame(chr, desc, locker, password)
                            chr.miniGame = game
                            game.pieceType = type
                            game.gameType = "omok"
                            chr.map.addMapObject(game)
                            chr.map.broadcastMessage(MiniGamePacket.addOmokBox(chr, locker, 1, 0))
                            game.sendOmok(c, type)
                        }
                        2 -> { // Match card
                            val desc = slea.readGameASCIIString()
                            val locker = slea.readByte().toInt()
                            val password = if (locker == 1) slea.readGameASCIIString() else null
                            val type = slea.readByte().toInt()
                            val game = MiniGame(chr, desc, locker, password)
                            game.matchesToWin = when (type) {
                                0 -> 6
                                1 -> 10
                                2 -> 15
                                else -> 6
                            }
                            game.gameType = "matchcard"
                            chr.miniGame = game
                            chr.map.addMapObject(game)
                            chr.map.broadcastMessage(MiniGamePacket.addMatchCardBox(chr, locker, 1, 0))
                        }
                        4, 5 -> { // Shop
                            if (chr.map.getMapObjectsInRange(chr.position, 23000.0, listOf(MapObjectType.SHOP, MapObjectType.HIRED_MERCHANT)).isNotEmpty())
                                return
                            val desc = slea.readGameASCIIString()
                            slea.skip(3)
                            val itemId = slea.readInt()
                            if ((chr.getInventory(InventoryType.CASH)?.countById(itemId) ?: 0) < 1) return
                            if (chr.mapId in 910000001..910000022 || itemId in 5030001..5030011 || itemId in 5140001..5140005) {
                                if (createType == 4) {
                                    val shop = PlayerShop(chr, desc)
                                    chr.playerShop = shop
                                    chr.map.addMapObject(shop)
                                    shop.sendShop(c)
                                    c.announce(InteractPacket.getPlayerShopRemoveVisitor(1))
                                } else {
                                    val merchant = HiredMerchant(chr, itemId, desc)
                                    chr.hiredMerchant = merchant
                                    chr.client.getChannelServer().addHiredMerchant(chr.id, merchant)
                                    chr.announce(CashPacket.getHiredMerchant(chr, merchant, true))
                                }
                            }
                        }
                    }
                }
                Action.INVITE -> {
                    val otherPlayer = slea.readInt()
                    chr.trade?.host = true
                    chr.map.getCharacterById(otherPlayer)?.let { Trade.inviteTrade(chr, it) }
                }
                Action.DECLINE -> Trade.declineTrade(chr)
                Action.VISIT -> {
                    if (chr.trade != null && chr.trade?.partner != null) {
                        chr.trade?.host = false
                        chr.trade?.partner?.chr?.let { Trade.visitTrade(chr, it) }
                    } else {
                        val oid = slea.readInt()
                        val ob = chr.map.mapObjects[oid]
                        if (ob is PlayerShop) {
                            if (ob.isBanned(chr.name)) {
                                chr.dropMessage(1, "You have been banned from this store.")
                                return
                            }
                            if (ob.hasFreeSlot() && !ob.isVisitor(c.player)) {
                                ob.addVisitor(chr)
                                chr.playerShop = ob
                                ob.sendShop(c)
                            }
                        } else if (ob is MiniGame) {
                            slea.readByte()
                            if (ob.locker == 1) {
                                if (ob.password != slea.readGameASCIIString()) {
                                    c.player?.dropMessage(1, "패스워드가 틀렸습니다.")
                                    return
                                }
                            }
                            if (ob.hasFreeSlot() && !ob.isVisitor(chr)) {
                                ob.addVisitor(chr)
                                chr.miniGame = ob
                                when (ob.gameType) {
                                    "omok" -> ob.sendOmok(c, ob.pieceType)
                                    "matchcard" -> ob.sendMatchCard(c, ob.pieceType)
                                }
                            } else {
                                chr.client.announce(MiniGamePacket.getMiniGameFull())
                            }
                        } else if (ob is HiredMerchant && chr.hiredMerchant == null) {
                            if (ob.isOwner(chr)) {
                                ob.open = false
                                ob.removeAllVisitors("")
                                c.announce(CashPacket.getHiredMerchant(chr, ob, false))
                            } else if (!ob.open) {
                                chr.dropMessage(1, "This shop is in maintenance, please come by later.") //TODO: English
                                return
                            } else if (ob.getFreeSlot() == -1) {
                                chr.dropMessage(1, "This shop has reached it's maximum capcity, please come by later.") //TODO: English
                                return
                            } else {
                                ob.addVisitor(chr)
                                c.announce(CashPacket.getHiredMerchant(chr, ob, false))
                            }
                            chr.hiredMerchant = ob
                        }
                    }
                }
                Action.CHAT -> {
                    val merchant = chr.hiredMerchant
                    when {
                        chr.trade != null -> {
                            chr.trade?.host?.let { chr.trade?.chat(slea.readGameASCIIString(), it) }
                        }
                        chr.playerShop != null -> {
                            chr.playerShop?.chat(c, slea.readGameASCIIString())
                        }
                        chr.miniGame != null -> {
                            chr.miniGame?.chat(c, slea.readGameASCIIString())
                        }
                        chr.hiredMerchant != null -> {
                            val message = "${chr.name} : ${slea.readGameASCIIString()}"
                            val slot = ((merchant?.getVisitorSlot(chr) ?: -1) + 1).toByte()
                            merchant?.messages?.add(Pair(message, slot))
                            merchant?.broadcastToVisitors(CashPacket.hiredMerchantChat(message, slot))
                        }
                    }
                }
                Action.EXIT -> {
                    if (chr.trade != null) {
                        Trade.cancelTrade(chr)
                    } else {
                        val shop = chr.playerShop
                        val game = chr.miniGame
                        val merchant = chr.hiredMerchant
                        if (shop != null) {
                            if (shop.isOwner(chr)) {
                                shop.items.forEach {
                                    if (it.bundles > 2) {
                                        val iItem = it.item.copy()
                                        iItem.quantity = (it.bundles * iItem.quantity).toShort()
                                        addFromDrop(c, iItem, false)
                                    } else if (it.doesExist) {
                                        addFromDrop(c, it.item, true)
                                    }
                                }
                                chr.map.broadcastMessage(CharacterPacket.removeCharBox(chr))
                                shop.removeVisitors()
                            } else {
                                shop.removeVisitor(chr)
                            }
                            chr.playerShop = null
                        } else if (game != null) {
                            chr.miniGame = null
                            if (game.isOwner(chr)) {
                                chr.map.broadcastMessage(CharacterPacket.removeCharBox(chr))
                                game.visitor?.client?.announce(MiniGamePacket.getMiniGameClose(true))
                            } else {
                                game.removeVisitor(chr)
                            }
                        } else if (merchant != null) {
                            merchant.removeVisitor(chr)
                            chr.hiredMerchant = null
                        }
                    }
                }
                Action.OPEN -> {
                    val shop = chr.playerShop
                    val merchant = chr.hiredMerchant
                    if (shop != null && shop.isOwner(c.player)) {
                        slea.readByte()
                        c.player?.let { chr.map.broadcastMessage(CharacterPacket.addCharBox(it, 4)) }
                    } else if (merchant != null && merchant.isOwner(chr)) {
                        chr.hasMerchant = true
                        merchant.open = true
                        chr.map.addMapObject(merchant)
                        chr.hiredMerchant = null
                        chr.map.broadcastMessage(CashPacket.spawnHiredMerchant(merchant))
                        slea.readByte()
                    }
                }
                Action.READY -> chr.miniGame?.broadcast(MiniGamePacket.getMiniGameReady())
                Action.UN_READY -> chr.miniGame?.broadcast(MiniGamePacket.getMiniGameUnReady())
                Action.START -> {
                    val game = chr.miniGame ?: return
                    if (game.gameType == "omok") {
                        game.broadcast(MiniGamePacket.getMiniGameStart(game.loser))
                        chr.map.broadcastMessage(MiniGamePacket.addOmokBox(game.owner, 0, 2, 1))
                    }
                    if (game.gameType == "matchcard") {
                        game.shuffleList()
                        game.broadcast(MiniGamePacket.getMatchCardStart(game, game.loser))
                        chr.map.broadcastMessage(MiniGamePacket.addMatchCardBox(game.owner, 0, 2, 1))
                    }
                }
                Action.GIVE_UP -> {
                    val game = chr.miniGame ?: return
                    if (game.gameType == "omok") {
                        if (game.isOwner(c.player)) {
                            game.broadcast(MiniGamePacket.getMiniGameOwnerForfeit(game))
                        } else {
                            game.broadcast(MiniGamePacket.getMiniGameVisitorForfeit(game))
                        }
                    }
                    if (game.gameType == "matchcard") {
                        if (game.isOwner(c.player)) {
                            game.broadcast(MiniGamePacket.getMatchCardVisitorWin(game))
                        } else {
                            game.broadcast(MiniGamePacket.getMatchCardOwnerWin(game))
                        }
                    }
                }
                Action.REQUEST_TIE -> {
                    val game = chr.miniGame ?: return
                    if (game.isOwner(c.player)) {
                        game.visitor?.client?.announce(MiniGamePacket.getMiniGameRequestTie())
                    } else {
                        game.owner.client.announce(MiniGamePacket.getMiniGameRequestTie())
                    }
                }
                Action.ANSWER_TIE -> {
                    val game = chr.miniGame ?: return
                    slea.readByte()
                    if (game.gameType == "omok") {
                        game.broadcast(MiniGamePacket.getMiniGameTie(game))
                    }
                    if (game.gameType == "matchcard") {
                        game.broadcast(MiniGamePacket.getMatchCardTie(game))
                    }
                }
                Action.SKIP -> {
                    val game = chr.miniGame ?: return
                    if (game.isOwner(c.player)) {
                        game.broadcast(MiniGamePacket.getMiniGameSkipOwner())
                    } else {
                        game.broadcast(MiniGamePacket.getMiniGameSkipVisitor())
                    }
                }
                Action.MOVE_OMOK -> {
                    val x = slea.readInt()
                    val y = slea.readInt()
                    val type = slea.readByte()// piece ( 1 or 2; Owner has one piece, visitor has another, it switches every game.)
                    chr.miniGame?.setPiece(x, y, type.toInt(), chr)
                }
                Action.SELECT_CARD -> {
                    val turn = slea.readByte().toInt()// 1st turn = 1; 2nd turn = 0
                    val slot = slea.readByte().toInt()
                    val game = chr.miniGame ?: return
                    val firstSlot = game.firstSlot
                    if (turn == 1) {
                        game.firstSlot = slot
                        if (game.isOwner(c.player)) {
                            game.visitor?.client?.announce(MiniGamePacket.getMatchCardSelect(game, turn, slot, firstSlot, turn))
                        } else {
                            game.owner.client.announce(MiniGamePacket.getMatchCardSelect(game, turn, slot, firstSlot, turn))
                        }
                    } else if ((game.getCardId(firstSlot + 1)) == (game.getCardId(slot + 1))) {
                        if (game.isOwner(c.player)) {
                            game.broadcast(MiniGamePacket.getMatchCardSelect(game, turn, slot, firstSlot, 2))
                            game.setOwnerPoints()
                        } else {
                            game.broadcast(MiniGamePacket.getMatchCardSelect(game, turn, slot, firstSlot, 3))
                            game.setVisitorPoints()
                        }
                    } else if (game.isOwner(c.player)) {
                        game.broadcast(MiniGamePacket.getMatchCardSelect(game, turn, slot, firstSlot, 0))
                    } else {
                        game.broadcast(MiniGamePacket.getMatchCardSelect(game, turn, slot, firstSlot, 1))
                    }
                }
                Action.SET_MESO -> chr.trade?.meso = slea.readInt()
                Action.SET_ITEMS -> {
                    val ivType = InventoryType.getByType(slea.readByte())
                    val item = chr.getInventory(ivType)?.getItem(slea.readShort().toByte()) ?: return
                    val quantity = slea.readShort()
                    val targetSlot = slea.readByte()
                    if (chr.trade != null) {
                        if (quantity <= item.quantity && quantity >= 0 || ItemConstants.isRechargeable(item.itemId)) {
                            if (ItemInformationProvider.isDropRestricted(item.itemId)) { // ensure that undroppable items do not make it to the trade window
                                if (!(item.flag and ItemConstants.KARMA == ItemConstants.KARMA || item.flag and ItemConstants.SPIKES == ItemConstants.SPIKES)) {
                                    c.announce(PacketCreator.enableActions())
                                    return
                                }
                            }
                            val tradeItem = item.copy()
                            if (ItemConstants.isRechargeable(item.itemId)) {
                                tradeItem.quantity = item.quantity
                                removeFromSlot(c, ivType, item.position, item.quantity,
                                    fromDrop = true,
                                    consume = false
                                )
                            } else {
                                tradeItem.quantity = quantity
                                removeFromSlot(c, ivType, item.position, quantity, fromDrop = true, consume = false)
                            }
                            tradeItem.position = targetSlot
                            chr.trade?.addItem(tradeItem)
                        }
                    }
                }
                Action.CONFIRM -> Trade.completeTrade(chr)
                Action.ADD_ITEM, Action.PUT_ITEM -> {
                    val type = InventoryType.getByType(slea.readByte())
                    val slot = slea.readShort().toByte()
                    val bundles = slea.readShort()
                    if ((chr.getInventory(type)?.getItem(slot)?.itemId?.let { chr.getItemQuantity(it, false) } ?: 0) < bundles || chr.getInventory(type)?.getItem(slot)?.flag == ItemConstants.UNTRADEABLE)
                        return
                    val perBundle = slea.readShort()
                    val price = slea.readInt()
                    if (perBundle < 0 || perBundle * bundles > 2000 || bundles < 0 || price < 0) return
                    val ivItem = chr.getInventory(type)!!.getItem(slot)!!
                    val sellItem = ivItem.copy()
                    if (chr.getItemQuantity(ivItem.itemId, false) < perBundle * bundles) return
                    sellItem.quantity = perBundle
                    val item = PlayerShopItem(sellItem, bundles, price)
                    val shop = chr.playerShop
                    val merchant = chr.hiredMerchant
                    if (shop != null && shop.isOwner(c.player)) {
                        if (ivItem != null && ivItem.quantity >= bundles * perBundle) {
                            shop.addItem(item)
                            c.announce(InteractPacket.getPlayerShopItemUpdate(shop))
                        }
                    } else if (merchant != null && merchant.isOwner(c.player!!)) {
                        merchant.addItem(item)
                        c.announce(CashPacket.updateHiredMerchant(merchant, c.player))
                    }
                    if (ItemConstants.isRechargeable(ivItem.itemId)) {
                        removeFromSlot(c, type, slot, ivItem.quantity, fromDrop = true, consume = false)
                    } else {
                        removeFromSlot(c, type, slot, (bundles * perBundle).toShort(), fromDrop = true, consume = false)
                    }
                }
                Action.REMOVE_ITEM -> {
                    val shop = chr.playerShop
                    if (shop != null && shop.isOwner(c.player)) {
                        val slot = slea.readShort().toInt()
                        val (item1, bundles) = shop.items[slot]
                        val ivItem = item1.copy()
                        shop.removeItem(slot)
                        ivItem.quantity = bundles
                        addFromDrop(c, ivItem, false)
                        c.announce(InteractPacket.getPlayerShopItemUpdate(shop))
                    }
                }
                Action.MERCHANT_ORGANIZE -> {
                    val merchant = chr.hiredMerchant
                    if (!merchant!!.isOwner(chr)) return

                    if (chr.merchantMeso > 0) {
                        val possible = Int.MAX_VALUE - chr.merchantMeso
                        if (possible > 0) {
                            if (possible < chr.merchantMeso) {
                                chr.gainMeso(possible, show = false, enableActions = false, inChat = false)
                                chr.merchantMeso = chr.merchantMeso - possible
                            } else {
                                chr.gainMeso(chr.merchantMeso, show = false, enableActions = false, inChat = false)
                                chr.merchantMeso = 0
                            }
                        }
                    }
                    for (i in merchant.items.indices) {
                        if (!merchant.items[i].doesExist) merchant.removeFromSlot(i)
                    }
                    if (merchant.items.isEmpty()) {
                        c.announce(CashPacket.hiredMerchantOwnerLeave())
                        c.announce(CashPacket.leaveHiredMerchant(0x00, 0x03))
                        merchant.closeShop(c, false)
                        chr.hasMerchant = false
                        return
                    }
                    c.announce(CashPacket.updateHiredMerchant(merchant, chr))
                }
                Action.BUY, Action.MERCHANT_BUY -> {
                    val item = slea.readByte().toInt()
                    val quantity = slea.readShort()
                    val shop = chr.playerShop
                    val merchant = chr.hiredMerchant
                    if (merchant != null && merchant.ownerName == chr.name) return
                    if (shop != null && shop.isVisitor(c.player)) {
                        shop.buy(c, item, quantity)
                        shop.broadcast(InteractPacket.getPlayerShopItemUpdate(shop))
                    } else if (merchant != null) {
                        merchant.buy(c, item, quantity.toInt())
                        merchant.broadcastToVisitors(CashPacket.updateHiredMerchant(merchant, c.player))
                    }
                }
                Action.TAKE_ITEM_BACK -> {
                    val merchant = chr.hiredMerchant
                    if (merchant != null && merchant.isOwner(c.player!!)) {
                        val slot = slea.readShort().toInt()
                        val (iItem, bundles) = merchant.items[slot]
                        if (bundles > 0) {
                            iItem.quantity = (iItem.quantity * bundles).toShort()
                            addFromDrop(c, iItem, true)
                        }
                        merchant.removeFromSlot(slot)
                        c.announce(CashPacket.updateHiredMerchant(merchant, c.player))
                    }
                }
                Action.CLOSE_MERCHANT -> {
                    val merchant = chr.hiredMerchant
                    if (merchant != null && merchant.isOwner(chr)) {
                        c.announce(CashPacket.hiredMerchantOwnerLeave())
                        c.announce(CashPacket.leaveHiredMerchant(0x00, 0x03))
                        merchant.closeShop(c, false)
                        chr.hasMerchant = false
                    }
                }
                Action.MAINTENANCE_OFF -> {
                    val merchant = chr.hiredMerchant
                    if (merchant?.items?.isEmpty() == true && merchant.isOwner(chr)) {
                        merchant.closeShop(c, false)
                        chr.hasMerchant = false
                    }
                    if (merchant != null && merchant.isOwner(chr)) {
                        merchant.messages.clear()
                        merchant.open = true
                    }
                    chr.hiredMerchant = null
                    c.announce(PacketCreator.enableActions())
                }
                Action.BAN_PLAYER -> {
                    if (chr.playerShop != null && chr.playerShop?.isOwner(chr) == true) {
                        chr.playerShop!!.banPlayer(slea.readGameASCIIString())
                    }
                }
                Action.MINIGAME_BAN_PLAYER -> {
                    if (chr.miniGame != null && chr.miniGame?.isOwner(c.player) == true) {
                        chr.miniGame?.banVisitor()
                    }
                }
                else -> {}
            }
        }
    }
}