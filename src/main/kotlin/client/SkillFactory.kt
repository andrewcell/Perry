package client

import constants.skills.*
import mu.KLogging
import provider.Data
import provider.DataProvider
import provider.DataProviderFactory
import provider.DataTool
import server.StatEffect
import server.life.Element
import tools.ServerJSON
import tools.StringXmlParser
import java.io.File
import kotlin.collections.first as first1

class SkillFactory {
    companion object : KLogging() {
        val skills = mutableMapOf<Int, Skill>()
        private val dataSource: DataProvider = DataProviderFactory.getDataProvider(DataProviderFactory.fileInWzPath("Skill.wz"))

        fun getSkill(id: Int?): Skill? = skills[id]

        fun getSkillName(id: Int): String? {
            val data = DataProviderFactory.getDataProvider(File("${ServerJSON.settings.wzPath}/String.wz")).getData("Skill.img")
            val skill = StringBuilder()
            skill.append(id)
            if (skill.length == 4) {
                skill.delete(0, 4)
                skill.append("000").append(id)
            }
            val strData = data?.getChildByPath(skill.toString())
            strData?.children?.forEach {
                if (it.name == "name") {
                    return DataTool.getStringNullable(it, null)
                }
            }
            return null
        }

        private fun loadFromData(id: Int, data: Data): Skill {
            val ret = Skill(id)
            var isBuff = false
            val skillType = DataTool.getInt("skillType", data, -1)
            val elem = DataTool.getStringNullable("elemAttr", data, null)
            ret.element = if (elem != null) {
                Element.getFromChar(elem.elementAt(0))
            } else Element.NEUTRAL
            val effect = data.getChildByPath("effect")
            if (skillType != -1) {
                if (skillType == 2) isBuff = true
            } else {
                val action = data.getChildByPath("action")
                var isAction = false
                if (action == null) {
                    if (data.getChildByPath("prepare/action") != null) {
                        isAction = true
                    } else {
                        when (id) {
                            5201001, 5221009 -> isAction = true
                        }
                    }
                } else isAction = true
                ret.action = isAction
                val hit = data.getChildByPath("hit")
                val ball = data.getChildByPath("ball")
                isBuff = effect != null && hit == null && ball == null
                isBuff = isBuff or (action != null && DataTool.getString("0", action, "") == "alert2")
                when (id) {
                    Hero.RUSH, Paladin.RUSH, DarkKnight.RUSH, DragonKnight.SACRIFICE, FPMage.EXPLOSION, FPMage.POISON_MIST, Cleric.HEAL, Ranger.MORTAL_BLOW, Sniper.MORTAL_BLOW, Assassin.DRAIN, Hermit.SHADOW_WEB, Bandit.STEAL, Shadower.SMOKE_SCREEN, SuperGM.HEAL_PLUS_DISPEL, Hero.MONSTER_MAGNET, Paladin.MONSTER_MAGNET, DarkKnight.MONSTER_MAGNET, Gunslinger.RECOIL_SHOT, Marauder.ENERGY_DRAIN -> isBuff =
                        false
                    Beginner.RECOVERY, Beginner.NIMBLE_FEET, Beginner.MONSTER_RIDER, Beginner.ECHO_OF_HERO, Swordsman.IRON_BODY, Fighter.AXE_BOOSTER, Fighter.POWER_GUARD, Fighter.RAGE, Fighter.SWORD_BOOSTER, Crusader.ARMOR_CRASH, Crusader.COMBO, Hero.ENRAGE, Hero.HEROS_WILL, Hero.M_WARRIOR, Hero.STANCE, Page.BW_BOOSTER, Page.POWER_GUARD, Page.SWORD_BOOSTER, Page.THREATEN, WhiteKnight.BW_FIRE_CHARGE, WhiteKnight.BW_ICE_CHARGE, WhiteKnight.BW_LIT_CHARGE, WhiteKnight.MAGIC_CRASH, WhiteKnight.SWORD_FIRE_CHARGE, WhiteKnight.SWORD_ICE_CHARGE, WhiteKnight.SWORD_LIT_CHARGE, Paladin.BW_HOLY_CHARGE, Paladin.HEROS_WILL, Paladin.M_WARRIOR, Paladin.STANCE, Paladin.SWORD_HOLY_CHARGE, Spearman.HYPER_BODY, Spearman.IRON_WILL, Spearman.POLEARM_BOOSTER, Spearman.SPEAR_BOOSTER, DragonKnight.DRAGON_BLOOD, DragonKnight.POWER_CRASH, DarkKnight.AURA_OF_BEHOLDER, DarkKnight.BEHOLDER, DarkKnight.HEROS_WILL, DarkKnight.HEX_OF_BEHOLDER, DarkKnight.M_WARRIOR, DarkKnight.STANCE, Magician.MAGIC_GUARD, Magician.MAGIC_ARMOR, FPWizard.MEDITATION, FPWizard.SLOW, FPMage.SEAL, FPMage.SPELL_BOOSTER, FPArchMage.HEROS_WILL, FPArchMage.INFINITY, FPArchMage.MANA_REFLECTION, FPArchMage.M_WARRIOR, ILWizard.MEDITATION, ILMage.SEAL, ILWizard.SLOW, ILMage.SPELL_BOOSTER, ILArchMage.HEROS_WILL, ILArchMage.INFINITY, ILArchMage.MANA_REFLECTION, ILArchMage.M_WARRIOR, Cleric.INVINCIBLE, Cleric.BLESS, Priest.DISPEL, Priest.DOOM, Priest.HOLY_SYMBOL, Priest.MYSTIC_DOOR, Bishop.HEROS_WILL, Bishop.HOLY_SHIELD, Bishop.INFINITY, Bishop.MANA_REFLECTION, Bishop.M_WARRIOR, Archer.FOCUS, Hunter.BOW_BOOSTER, Hunter.SOUL_ARROW, Ranger.PUPPET, Bowmaster.CONCENTRATE, Bowmaster.HEROS_WILL, Bowmaster.M_WARRIOR, Bowmaster.SHARP_EYES, Crossbowman.CROSSBOW_BOOSTER, Crossbowman.SOUL_ARROW, Sniper.PUPPET, Marksman.BLIND, Marksman.HEROS_WILL, Marksman.M_WARRIOR, Marksman.SHARP_EYES, Rogue.DARK_SIGHT, Assassin.CLAW_BOOSTER, Assassin.HASTE, Hermit.MESO_UP, Hermit.SHADOW_PARTNER, NightLord.HEROS_WILL, NightLord.M_WARRIOR, NightLord.NINJA_AMBUSH, NightLord.SHADOW_STARS, Bandit.DAGGER_BOOSTER, Bandit.HASTE, ChiefBandit.MESO_GUARD, ChiefBandit.PICKPOCKET, Shadower.HEROS_WILL, Shadower.M_WARRIOR, Shadower.NINJA_AMBUSH, Pirate.DASH, Marauder.TRANSFORMATION, Buccaneer.SUPER_TRANSFORMATION, Corsair.BATTLE_SHIP, GM.HASTE, GM.HOLY_SYMBOL, GM.BLESS, GM.HIDE, SuperGM.HASTE, SuperGM.HOLY_SYMBOL, SuperGM.BLESS, SuperGM.HIDE, SuperGM.HYPER_BODY -> isBuff =
                        true
                }
            }
            data.getChildByPath("level")?.forEach { level ->
                ret.effects.add(StatEffect.loadSkillEffectFromData(level, id, isBuff))
            }
            ret.animationTime = 0
            effect?.forEach { e ->
                ret.animationTime += DataTool.getInt("delay", e, 0)
            }
            ret.coolTime = DataTool.getInt("cooltime", data, 0)
            return ret
        }

        fun getSkillDataByJobId(jobId: Int): Data? {
            val file = dataSource.root.files.first1 { it.name == "$jobId.img" }
            return dataSource.getData(file.name)
        }

        fun loadAllSkills() {
            val root = dataSource.root
            var skillId: Int
            root.files.forEach { topDir ->
                if (topDir.name != "") logger.trace { "Entering Skill.wz -  ${topDir.name}" }
                if (topDir.name.length <= 8) {
                    dataSource.getData(topDir.name)?.forEach { data ->
                        if (data.name == "skill") {
                            data.forEach { data2 ->
                                if (data2 != null) {
                                    skillId = data2.name.toInt()
                                    val skill = loadFromData(skillId, data2)
                                    val skillName = getSkillName(skill.id) ?: "Unknown"
                                    StringXmlParser.addSkillEntry(skillId, skillName)
                                    logger.trace { "Skill: ${skill.id} - $skillName" }
                                    skills[skillId] = skill
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}