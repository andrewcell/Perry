package constants

import client.inventory.InventoryType

class ItemConstants {
    companion object {
        const val LOCK = 0x01
        const val SPIKES = 0x02
        const val COLD = 0x04
        const val UNTRADEABLE = 0x08
        const val KARMA = 0x10
        const val PET_COME = 0x80
        const val UNKNOWN_SKILL = 0x100
        const val ITEM_ARMOR_EXP = (1 / 350000).toFloat()
        const val ITEM_WEAPON_EXP = (1 / 700000).toFloat()
        const val EXPIRING_ITEMS = true

        fun getFlagByInt(type: Int) = when (type) {
            128 -> PET_COME
            256 -> UNKNOWN_SKILL
            else -> 0
        }

        fun isThrowingStar(itemId: Int) = itemId / 10000 == 207

        fun isBullet(itemId: Int) = itemId / 10000 == 233

        fun isRechargeable(itemId: Int) = (itemId / 10000 == 233 || itemId / 10000 == 207)

        fun isArrowForCrossBow(itemId: Int) = itemId / 1000 == 2061

        fun isArrowForBow(itemId: Int) = itemId / 1000 == 2060

        fun isPet(itemId: Int) = itemId / 1000 == 5000

        fun getInventoryType(itemId: Int): InventoryType {
            val type = itemId / 1000000
            return if (type < 1 || type > 5) InventoryType.UNDEFINED
            else InventoryType.getByType(type.toByte())
        }
    }
}