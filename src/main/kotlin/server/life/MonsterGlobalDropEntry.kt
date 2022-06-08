package server.life

data class MonsterGlobalDropEntry(
    override val itemId: Int,
    override val chance: Int,
    val continent: Int,
    val dropType: Byte,
    override val minimum: Int,
    override val maximum: Int,
    override val questId: Short
): MonsterDropEntry(itemId, chance, minimum, maximum, questId)