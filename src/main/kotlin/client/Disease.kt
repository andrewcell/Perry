package client

enum class Disease(val value: Long) {
    NULL(0x0),
    SLOW(0x100000000L),
    SEDUCE(0x80),
    FISHABLE(0x100),
    CONFUSE(0x80000),
    STUN(0x20000L),
    POISON(0x40000L),
    SEAL(0x80000L),
    DARKNESS(0x100000L),
    WEAKEN(0x40000000L),
    CURSE(0x80000000L);
}