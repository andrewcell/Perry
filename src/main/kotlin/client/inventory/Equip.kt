package client.inventory

import client.Client
import server.ItemInformationProvider
import tools.packet.CharacterPacket
import tools.packet.ItemPacket

class Equip(id: Int, position: Byte, var upgradeSlots: Byte = -1) : Item(id, position, 1) {
    enum class ScrollResult(val value: Int) {
        FAIL(0), SUCCESS(1), CURSE(2);
    }
    var itemExp: Float = 0F
    var itemLevel: Byte = 1
    var isWearing = false
    var level: Byte = 0
    var str: Short = 0
    var dex: Short = 0
    var int: Short = 0
    var luk: Short = 0
    var hp: Short = 0
    var mp: Short = 0
    var watk: Short = 0
    var matk: Short = 0
    var wdef: Short = 0
    var mdef: Short = 0
    var acc: Short = 0
    var avoid: Short = 0
    var hands: Short = 0
    var speed: Short = 0
    var jump: Short = 0
    var vicious: Short = 0
    var ringId = -1

    override fun copy(): Item {
        val ret = Equip(itemId, position, upgradeSlots)
        ret.str = str
        ret.dex = dex
        ret.int = int
        ret.luk = luk
        ret.hp = hp
        ret.mp = mp
        ret.matk = matk
        ret.mdef = mdef
        ret.watk = watk
        ret.wdef = wdef
        ret.acc = acc
        ret.avoid = avoid
        ret.hands = hands
        ret.speed = speed
        ret.jump = jump
        ret.flag = flag
        ret.vicious = vicious
        ret.upgradeSlots = upgradeSlots
        ret.itemLevel = itemLevel
        ret.itemExp = itemExp
        ret.level = level
        ret.log = log
        ret.owner = owner
        ret.quantity = quantity
        ret.expiration = expiration
        ret.giftFrom = giftFrom
        return ret
    }

    private fun gainLevel(c: Client, timeless: Boolean) {
        val stats = ItemInformationProvider.getItemLevelUpStats(itemId, itemLevel.toInt(), timeless)
        stats.forEach { (name, value) ->
            val v = value.toShort()
            when (name) {
                "incDEX" -> dex = (dex + v).toShort()
                "incSTR" -> str = (str + value).toShort()
                "incINT" -> int = (int + value).toShort()
                "incLUK" -> luk = (luk + value).toShort()
                "incMHP" -> hp = (hp + value).toShort()
                "incMMP" -> mp = (mp + value).toShort()
                "incPAD" -> watk = (watk + value).toShort()
                "incMAD" -> matk = (matk + value).toShort()
                "incPDD" -> wdef = (wdef + value).toShort()
                "incMDD" -> mdef = (mdef + value).toShort()
                "incEVA" -> avoid = (avoid + value).toShort()
                "incACC" -> acc = (acc + value).toShort()
                "incSpeed" -> speed = (speed + value).toShort()
                "incJump" -> jump = (jump + value).toShort()
            }
        }
        itemLevel++
        c.announce(ItemPacket.showEquipmentLevelUp())
        c.player?.let {
            it.map.broadcastMessage(it, CharacterPacket.showForeignEffect(it.id, 15))
            it.forceUpdateItem(this)
        }
    }

    fun gainItemExp(c: Client, gain: Int, timeless: Boolean) {
        val expNeeded = if (timeless) 10 * itemLevel + 70 else 5 * itemLevel + 65
        val modifier = 364.0 / expNeeded
        val exp = (expNeeded / (1000000 * modifier * modifier)) * gain
        itemExp += exp.toFloat()
        if (itemExp >= 364) {
            itemExp -= 364
            gainLevel(c, timeless)
        } else {
            c.player?.forceUpdateItem(this)
        }
    }

    override var quantity: Short
        get() = super.quantity
        set(value) {
            if (value < 0 || quantity > 1) return
            super.quantity = value
        }

    override fun getType() = 1
}