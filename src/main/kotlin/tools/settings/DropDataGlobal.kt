package tools.settings
/*
  {
    "id": 1,
    "continent": 0,
    "dropType": 4,
    "itemid": 4000047,
    "minimum_quantity": 1,
    "maximum_quantity": 1,
    "questid": 0,
    "chance": 16000,
    "comments": "마우스"
  },
 */
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
