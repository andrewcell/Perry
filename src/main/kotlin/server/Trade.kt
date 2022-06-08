package server

import client.Character
import client.inventory.InventoryType
import client.inventory.Item
import constants.ItemConstants
import mu.KLoggable
import tools.PacketCreator
import tools.packet.InteractPacket
import kotlin.math.roundToInt

class Trade(val number: Byte, val chr: Character) : KLoggable {
    override val logger = logger()
    var locked = false
    var partner: Trade? = null
        set(value) {
            if (locked) return else field = value
        }
    private var exchangeItems = mutableListOf<Item>()
    var exchangeMeso = 0
    val items = mutableListOf<Item>()
    var host = false
    var meso = 0
        set(value) {
            if (locked) logger.warn { "Trade is locked but meso value changed. CharacterId: ${chr.id}" }
            if (value < 0) logger.warn { "trying to trade < 0 mesos. CharacterId: ${chr.id}" }
            if (chr.meso.get() >= value) {
                chr.gainMeso(-value, show = false, enableActions = true, inChat = false)
                field += value
                chr.client.announce(InteractPacket.getTradeMesoSet(0, field))
                partner?.chr?.client?.announce(InteractPacket.getTradeMesoSet(1, field))
            }
        }

    fun lock() {
        lock()
        partner?.chr?.client?.announce(InteractPacket.getTradeConfirmation())
    }

    fun complete1() {
        exchangeItems = partner?.items ?: mutableListOf()
        exchangeMeso = partner?.meso ?: 0
    }

    fun complete2() {
        items.clear()
        meso = 0
        exchangeItems.forEach { item ->
            if (item.flag.and(ItemConstants.KARMA) == ItemConstants.KARMA) {
                item.flag = item.flag.xor(ItemConstants.KARMA) //items with scissors of karma used on them are reset once traded
            } else if (item.getType() == 2 && item.flag.and(ItemConstants.SPIKES) == ItemConstants.SPIKES) {
                item.flag = item.flag.xor(ItemConstants.SPIKES)
                InventoryManipulator.addFromDrop(chr.client, item, true)
            }
        }
        if (exchangeMeso > 0) {
            chr.gainMeso(exchangeMeso - getFee(exchangeMeso), show = true, enableActions = true, inChat = true)
        }
        exchangeMeso = 0
        exchangeItems.clear()
        chr.client.announce(InteractPacket.getTradeCompletion(number))
    }

    fun cancel() {
        items.forEach { item -> InventoryManipulator.addFromDrop(chr.client, item, true) }
        if (meso > 0) chr.gainMeso(meso, show = true, enableActions = true, inChat = true)
        meso = 0
        items.clear()
        exchangeMeso = 0
        exchangeItems.clear()
        chr.client.announce(InteractPacket.getTradeCancel(number))
    }

    fun addItem(item: Item) {
        items.add(item)
        chr.client.announce(InteractPacket.getTradeItemAdd(0, item))
        partner?.chr?.client?.announce(InteractPacket.getTradeItemAdd(1, item))
    }

    fun chat(message: String, host: Boolean) {
        chr.client.announce(InteractPacket.getTradeChat(chr, message, host))
        partner?.chr?.client?.announce(InteractPacket.getTradeChat(chr, message, host))
    }

    private fun isFitsInInventory(): Boolean {
        val mii = ItemInformationProvider
        val neededSlots = mutableMapOf<InventoryType, Int>()
        exchangeItems.forEach { item ->
            val type = mii.getInventoryType(item.itemId)
            if (neededSlots[type] == null) {
                neededSlots[type] = 1
            } else {
                neededSlots[type] = neededSlots[type]!!.plus(1)
            }
        }
        neededSlots.entries.forEach { entry ->
            if (chr.getInventory(entry.key)?.isFull(entry.value - 1) == true) {
                return false
            }
        }
        return true
    }

    companion object {
        fun cancelTrade(c: Character) {
            c.trade?.cancel()
            c.trade?.partner?.cancel()
            c.trade?.partner?.chr?.trade = null
            c.trade = null
        }

        fun startTrade(c: Character) {
            if (c.trade == null) {
                c.trade = Trade(0, c)
                c.client.announce(InteractPacket.getTradeStart(c.client, c.trade, 0))
            } else {
                c.message("이미 교환중입니다.")
            }
        }

        fun inviteTrade(c1: Character, c2: Character) {
            if (c2.trade == null) {
                c2.trade = Trade(1, c2)
                c2.trade?.partner = c1.trade
                c1.trade?.partner = c2.trade
                c2.client.announce(InteractPacket.getTradeInvite(c1))
            } else {
                c1.message("대상은 이미 교환중입니다.")
                cancelTrade(c1)
            }
        }

        fun visitTrade(c1: Character, c2: Character) {
            val trade1 = c1.trade
            val trade2 = c2.trade
            if (trade1 != null && trade1.partner == trade2 && trade2 != null && trade2.partner == trade1) {
                c2.client.announce(InteractPacket.getTradePartnerAdd(c1))
                c1.client.announce(InteractPacket.getTradeStart(c1.client, trade1, 1))
            } else {
                c1.message("상대방이 이미 교환창을 나가셨습니다.")
            }
        }

        fun declineTrade(c: Character) {
            val trade = c.trade
            if (trade != null) {
                if (trade.partner != null) {
                    val other = trade.partner?.chr
                    other?.trade?.cancel()
                    other?.trade = null
                    other?.message("${c.name} 님이 교환 요청을 거절하였습니다.")
                }
                trade.cancel()
                c.trade = null
            }
        }

        fun completeTrade(c: Character) {
            c.trade?.lock()
            val local = c.trade
            val partner = local?.partner
            if (partner?.locked == true) {
                local.complete1()
                partner.complete1()
                if (!local.isFitsInInventory() || !partner.isFitsInInventory()) {
                    cancelTrade(c)
                    c.message("There is not enough inventory space to complete the trade.") //TODO: need to be translated.
                    partner.chr.message("There is not enough inventory space to complete the trade.")

                }
                if(local.chr.level < 15) {
                    if (local.chr.mesosTraded + local.exchangeMeso > 1000000) {
                        cancelTrade(c)
                        local.chr.client.announce(PacketCreator.sendMesoLimit())
                        return
                    } else {
                        local.chr.addMesosTraded(local.exchangeMeso)
                    }
                } else if ((c.trade?.chr?.level ?: 15) < 15) {
                    if (c.mesosTraded + (c.trade?.exchangeMeso ?: 0) > 1000000) {
                        cancelTrade(c)
                        c.client.announce(PacketCreator.sendMesoLimit())
                        return
                    } else {
                        c.addMesosTraded(local.exchangeMeso)
                    }
                }
                local.complete2()
                partner.complete2()
                partner.chr.trade = null
                c.trade = null
            }
        }

        fun getFee(meso: Int): Int {
            return when  {
                (meso >= 100000000) -> (0.06 * meso).roundToInt()
                (meso >= 25000000) -> meso / 20
                (meso >= 10000000) -> meso / 25
                (meso >= 5000000) -> (.03 * meso).roundToInt()
                (meso >= 1000000) -> (.018 * meso).roundToInt()
                (meso >= 100000) -> meso / 125
                else -> 0
            }
        }
    }
}