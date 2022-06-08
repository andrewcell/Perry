package server.life

open class MonsterDropEntry(
    open val itemId: Int,
    open val chance: Int,
    open val minimum: Int,
    open val maximum: Int,
    open val questId: Short
)
