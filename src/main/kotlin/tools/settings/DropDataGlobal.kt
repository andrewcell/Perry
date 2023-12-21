package tools.settings

import kotlinx.serialization.Serializable

/**
 * DropDataGlobal is a data class that represents the global drop data for a game entity.
 * It is annotated with @Serializable, which makes it compatible with Kotlin's serialization framework.
 * This class is typically used in the context of a game, where entities can drop items upon certain events.
 * The drop data is global, meaning it applies to all instances of the entity across all continents.
 *
 * @property id The unique identifier of the drop data.
 * @property continent The continent where the drop can occur (0 represents all continents).
 * @property dropType The type of the drop.
 * @property itemId The unique identifier of the item being dropped.
 * @property minimumQuantity The minimum quantity of the item that can be dropped.
 * @property maximumQuantity The maximum quantity of the item that can be dropped.
 * @property questId The unique identifier of the quest associated with the drop (0 if not associated with any quest).
 * @property chance The chance of the item being dropped (out of 100000).
 * @property comments Any additional comments or notes about the drop.
 */
@Serializable
data class DropDataGlobal(
    val id: Int,
    val continent: Int,
    val dropType: Int,
    val itemId: Int,
    val minimumQuantity: Int,
    val maximumQuantity: Int,
    val questId: Int,
    val chance: Int,
    val comments: String
)