package tools.settings

import kotlinx.serialization.Serializable

/**
 * MonsterCardDataDatabase is a data class that represents the monster card data in the database.
 * It is annotated with @Serializable, which makes it compatible with Kotlin's serialization framework.
 * This class is typically used in the context of a game, where item called monsters' cards that players can collect.
 *
 * @property id The unique identifier of the monster card data.
 * @property cardId The unique identifier of the card.
 * @property mobId The unique identifier of the monster that drops the card.
 */
@Serializable
data class MonsterCardDataDatabase(
    val id: Int,
    val cardId: Int,
    val mobId: Int
)