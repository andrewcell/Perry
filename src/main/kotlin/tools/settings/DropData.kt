package tools.settings

import kotlinx.serialization.Serializable

/**
 * DropData is a data class that represents the drop data for a game entity.
 * It is annotated with @Serializable, which makes it compatible with Kotlin's serialization framework.
 * This class is typically used in the context of a game, where entities can drop items upon certain events.
 *
 * @property id The unique identifier of the drop data.
 * @property dropperId The unique identifier of the entity that drops the item.
 * @property itemId The unique identifier of the item being dropped.
 * @property minimumQuantity The minimum quantity of the item that can be dropped.
 * @property maximumQuantity The maximum quantity of the item that can be dropped.
 * @property questId The unique identifier of the quest associated with the drop (0 if not associated with any quest).
 * @property chance The chance of the item being dropped (out of 100000).
 */
@Serializable
data class DropData(
    val id: Int,
    val dropperId: Int,
    val itemId: Int,
    val minimumQuantity: Int,
    val maximumQuantity: Int,
    val questId: Int,
    val chance: Int
)

/*
 {
    "id": 2999,
    "dropperid": 4230117,
    "itemid": 2061000,
    "minimum_quantity": 21,
    "maximum_quantity": 25,
    "questid": 0,
    "chance": 30000
  },
 */