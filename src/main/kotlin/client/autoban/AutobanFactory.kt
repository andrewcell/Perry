package client.autoban

import client.Character

/**
 * Autoban Factory
 * Types of ban caused. (With point and expire)
 *
 * @param points Ban point to cumulative
 * @param expire Expire to release ban
 * @param MOB_COUNT asasc
 */
enum class AutobanFactory(val points: Int = 1, val expire: Long = -1) {
    MOB_COUNT,
    MOVE_MONSTER,
    FIX_DAMAGE,
    HIGH_HP_HEALING,
    FAST_HEALING(15),

    //FAST_MP_HEALING(15),
    GACHA_EXP,
    TUBI(20, 15000),
    SHORT_ITEM_VAC,
    ITEM_VAC,
    ATTACK_FARAWAY_MONSTER,
    FAST_ATTACK(10, 30000),
    HEAL_ATTACKING_UNDEAD,
    MPCON(25, 30000);

    /**
     * Adds a point to the autoban manager for a specific reason.
     *
     * @param ban The autoban manager to which the point will be added.
     * @param reason The reason for adding the point.
     */
    fun addPoint(ban: AutobanManager, reason: String) = ban.addPoint(this, reason)

    /**
     * Initiates an autoban for a character if they are not a Game Master (GM).
     *
     * @param chr The character to be autobanned.
     * @param value The value or reason for the autoban.
     */
    fun autoban(chr: Character, value: String) {
        if (chr.isGM()) return
        chr.autoban("오토밴 됨. ($name: $value)", 1)
    }
}