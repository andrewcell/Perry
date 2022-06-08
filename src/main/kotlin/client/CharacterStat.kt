package client

enum class CharacterStat(val value: Int) {

    SKIN(0x1),
    FACE(0x2),
    HAIR(0x4),
    PET(0x8),
    LEVEL(0x10),
    JOB(0x20),
    STR(0x40),
    DEX(0x80),
    INT(0x100),
    LUK(0x200),
    HP(0x400),
    MAXHP(0x800),
    MP(0x1000),
    MAXMP(0x2000),
    AVAILABLEAP(0x4000),
    AVAILABLESP(0x8000),
    EXP(0x10000),
    FAME(0x20000),
    MESO(0x40000);

    companion object {
        fun getByValue(value: Int) = values().find { it.value == value }

        fun getBy5ByteEncoding(encoded: Int) = when (encoded) {
            64 -> STR
            128 -> DEX
            256 -> INT
            512 -> LUK
            else -> null
        }
    }
}