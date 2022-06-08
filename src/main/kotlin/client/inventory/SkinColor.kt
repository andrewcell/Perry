package client.inventory

enum class SkinColor(val id: Int) {
    NORMAL(0), DARK(1), BLACK(2), PALE(3), BLUE(4), GREEN(5), WHITE(9), PINK(10);

    companion object {
        fun getById(id: Int) = values().find { it.id == id }
    }
}