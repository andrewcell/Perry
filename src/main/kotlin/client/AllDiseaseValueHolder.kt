package client

import server.life.MobSkill

data class AllDiseaseValueHolder(val debuff: List<Pair<Disease, Int>>, val skill: MobSkill)
