package tools.settings

import kotlinx.serialization.Serializable

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