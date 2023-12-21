package tools.settings

import kotlinx.serialization.Serializable

/**
 * ReactorDropDatabase is a data class that represents the drop data for a reactor in the database.
 * It is annotated with @Serializable, which makes it compatible with Kotlin's serialization framework.
 * This class is typically used in the context of a game, where reactors can drop items upon certain events.
 *
 * @property reactorDropId The unique identifier of the reactor drop data.
 * @property reactorId The unique identifier of the reactor that drops the item.
 * @property itemId The unique identifier of the item being dropped.
 * @property change The chance of the item being dropped (out of 100000).
 * @property questId The unique identifier of the quest associated with the drop (0 if not associated with any quest).
 */
@Serializable
data class ReactorDropDatabase(
    val reactorDropId: Int,
    val reactorId: Int,
    val itemId: Int,
    val change: Int,
    val questId: Int
)