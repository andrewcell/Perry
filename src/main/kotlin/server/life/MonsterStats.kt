package server.life

data class MonsterStats(
    val hp: Int, val mp: Int, val exp: Int,
    val level: Int, val removeAfter: Int, var boss: Boolean,
    val explosiveReward: Boolean, val ffaLoot: Boolean, val undead: Boolean,
    val name: String, val buffToGive: Int, val cp: Int, val removeOnMiss: Boolean,
    val dropPeriod: Int, val tagColor: Int, val tagBgColor: Int,

) {
    var cool: Pair<Int, Int>? = null
    var banishInfo: LifeFactory.Companion.BanishInfo? = null
    var paDamage = -1
    var firstAttack = false
    var selfDestruction: LifeFactory.Companion.SelfDestruction? = null
    private val animationTimes = mutableMapOf<String, Int>()
    private val resistance = mutableMapOf<Element, ElementalEffectiveness>()
    var revives = mutableListOf<Int>()
    var skills = mutableListOf<Pair<Int, Int>>()
    private val loseItem = mutableListOf<LifeFactory.Companion.LoseItem>()

    fun addLoseItem(li: LifeFactory.Companion.LoseItem) = loseItem.add(li)

    fun addSkills(skillList: List<Pair<Int, Int>>) { skills += skillList.toMutableList() }

    fun getAnimationTime(name: String): Int = animationTimes[name] ?: 500

    fun setAnimationTime(name: String, delay: Int) = animationTimes.put(name, delay)

    fun getEffectiveness(e: Element) = resistance[e] ?: ElementalEffectiveness.NORMAL

    fun setEffectiveness(e: Element, ee: ElementalEffectiveness) = resistance.put(e, ee)

    fun removeEffectiveness(e: Element) = resistance.remove(e)

    fun hasSkill(skillId: Int, level: Int): Boolean = skills.find { it.first == skillId && it.second == level }?.let { true } ?: false

    fun isMobile() = animationTimes.containsKey("move") || animationTimes.containsKey("fly")
}