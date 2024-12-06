package client.inventory

/**
 * Enum class representing the different types of inventory in the game.
 *
 * Each enum constant represents a different type of inventory and is associated with a byte value.
 * The byte value is used for encoding and decoding the inventory type.
 *
 * @property type The byte value associated with the inventory type.
 * @see InventoryType.getBitfieldEncoding for the method that encodes the inventory type.
 * @see InventoryType.Companion.getByType for the method that decodes the inventory type.
 * @see InventoryType.Companion.getByWzName for the method that gets the inventory type by its WZ name.
 */
enum class InventoryType(val type: Byte) {
    UNDEFINED(0),
    EQUIP(1),
    USE(2),
    SETUP(3),
    ETC(4),
    CASH(5),
    EQUIPPED(-1); //Seems nx screwed something when removing an item T_T

    /**
     * Encodes the inventory type into a bitfield.
     *
     * This method is used to encode the inventory type into a bitfield. It shifts the number 2 to the left by the number of places equal to the byte value of the inventory type.
     *
     * @return The bitfield encoding of the inventory type.
     */
    fun getBitfieldEncoding() = 2 shl type.toInt()

    companion object {
        /**
         * Decodes the inventory type from a byte value.
         *
         * This method is used to decode the inventory type from a byte value. It searches the enum constants for a constant with the same byte value as the provided value.
         * If it finds a match, it returns the matching constant. If it doesn't find a match, it returns the UNDEFINED constant.
         *
         * @param type The byte value to decode into an inventory type.
         * @return The inventory type that matches the provided byte value, or UNDEFINED if no match is found.
         */
        fun getByType(type: Byte) = entries.find { it.type == type } ?: UNDEFINED

        /**
         * Gets the inventory type by its WZ name.
         *
         * This method is used to get the inventory type by its WZ name. It uses a when expression to match the provided name to the WZ name of an inventory type.
         * If it finds a match, it returns the matching constant. If it doesn't find a match, it returns the UNDEFINED constant.
         *
         * @param name The WZ name to match to an inventory type.
         * @return The inventory type that matches the provided WZ name, or UNDEFINED if no match is found.
         */
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