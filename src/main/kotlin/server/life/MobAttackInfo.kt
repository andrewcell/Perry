package server.life

data class MobAttackInfo(
    val mobId: Int,
    val attackId: Int,
    val isDeadlyAttack: Boolean,
    val mpBurn: Int,
    val diseaseSkill: Int,
    val diseaseLevel: Int,
    val mpCon: Int
)