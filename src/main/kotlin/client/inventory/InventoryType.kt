package client.inventory

enum class InventoryType(val type: Byte) {
    UNDEFINED(0),
    EQUIP(1),
    USE(2),
    SETUP(3),
    ETC(4),
    CASH(5),
    EQUIPPED(-1); //Seems nx screwed something when removing an item T_T

    fun getBitfieldEncoding() = 2 shl type.toInt()

    companion object {
        fun getByType(type: Byte) = values().find { it.type == type } ?: UNDEFINED

        fun getByWzName(name: String) = when (name) {
            "Install" -> SETUP
            "Consume" -> USE
            "Etc" -> ETC
            "Cash" -> CASH
            "Pet" -> CASH
            else -> UNDEFINED
        }
    }
}