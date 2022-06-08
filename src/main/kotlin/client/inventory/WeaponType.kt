package client.inventory

enum class WeaponType(val maxDamageMultiplier: Double) {
    NOT_A_WEAPON(0.0),
    AXE1H(4.4),
    AXE2H(4.8),
    BLUNT1H(4.4),
    BLUNT2H(4.8),
    BOW(3.4),
    CLAW(3.6),
    CROSSBOW(3.6),
    DAGGER(4.0),
    GUN(3.6),
    KNUCKLE(4.8),
    POLE_ARM(5.0),
    SPEAR(5.0),
    STAFF(3.6),
    SWORD1H(4.0),
    SWORD2H(4.6),
    WAND(3.6);
}