package client

/**
 * A data class representing an entry in a buddy list.
 * @param name
 * @param characterId
 * @param channel should be -1 if the buddy is offline
 * @param visible
 */
data class BuddyListEntry(
    val name: String,
    val group: String,
    val characterId: Int,
    var channel: Int,
    val visible: Boolean
) {
    fun isOnline() = channel >= 0
}