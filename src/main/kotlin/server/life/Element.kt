package server.life

enum class Element {
    NEUTRAL, FIRE, ICE, LIGHTING, POISON, HOLY;

    companion object  {
        fun getFromChar(c: Char): Element {
            return when (c.uppercaseChar()) {
                'F' -> FIRE
                'I' -> ICE
                'L' -> LIGHTING
                'S' -> POISON
                'H' -> HOLY
                'P' -> NEUTRAL
                else -> NEUTRAL
            }
        }
    }
}