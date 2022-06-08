package server.life

enum class ElementalEffectiveness(val value: Int) {
    NORMAL(0), IMMUNE(1), STRONG(2), WEAK(3), NEUTRAL(4);

    companion object {
        fun valueOf(value: Int) = values().find { it.value == value }
    }
}