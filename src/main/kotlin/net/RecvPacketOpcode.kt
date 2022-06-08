package net

import mu.KLogging
import tools.OpcodeProperties

enum class RecvPacketOpcode {
    CUSTOM_PACKET,

    LOGIN_SUCCESS,
    CLIENT_CHECK,
    LOGIN_PASSWORD, // 0x01
    CHARLIST_REQUEST, // 0x04
    CHAR_SELECT, // 0x05
    PLAYER_LOGGEDIN,
    CHECK_CHAR_NAME,
    CREATE_CHAR,
    DELETE_CHAR,
    PONG, // 0x09
    CLIENT_START_ERROR,
    TEST1,
    TEST2,
    CLIENT_ERROR,
    STRANGE_DATA,
    RELOG,
    REGISTER_PIC,
    CHAR_SELECT_WITH_PIC,
    VIEW_ALL_PIC_REGISTER,
    VIEW_ALL_WITH_PIC,
    CHANGE_MAP,
    CHANGE_CHANNEL,
    ENTER_CASHSHOP,
    MOVE_PLAYER,
    CANCEL_CHAIR,
    USE_CHAIR,
    CLOSE_RANGE_ATTACK,
    RANGED_ATTACK,
    MAGIC_ATTACK,
    TOUCH_MONSTER_ATTACK,
    TAKE_DAMAGE,
    GENERAL_CHAT,
    CLOSE_CHALKBOARD,
    FACE_EXPRESSION,
    USE_ITEMEFFECT,
    USE_DEATHITEM,
    MONSTER_BOOK_COVER,
    NPC_TALK,
    REMOTE_STORE,
    NPC_TALK_MORE,
    NPC_SHOP,
    STORAGE,
    HIRED_MERCHANT_REQUEST,
    FREDRICK_ACTION,
    DUEY_ACTION,
    ADMIN_SHOP,
    ITEM_SORT,
    ITEM_SORT2,
    ITEM_MOVE,
    USE_ITEM,
    CANCEL_ITEM_EFFECT,
    USE_SUMMON_BAG,
    PET_FOOD,
    USE_MOUNT_FOOD,
    SCRIPTED_ITEM,
    USE_CASH_ITEM,

    USE_CATCH_ITEM,
    USE_SKILL_BOOK,
    USE_TELEPORT_ROCK,
    USE_RETURN_SCROLL,
    COUPON_CODE,
    USE_UPGRADE_SCROLL,
    DISTRIBUTE_AP,
    AUTO_DISTRIBUTE_AP,
    HEAL_OVER_TIME,
    DISTRIBUTE_SP,
    SPECIAL_MOVE,
    CANCEL_BUFF,
    SKILL_EFFECT,
    MESO_DROP,
    GIVE_FAME,
    CHAR_INFO_REQUEST,
    SPAWN_PET,
    CANCEL_DEBUFF,
    CHANGE_MAP_SPECIAL,
    USE_INNER_PORTAL,
    TROCK_ADD_MAP,
    REPORT,
    QUEST_ACTION,
    //lolno
    SKILL_MACRO,
    SPOUSE_CHAT,
    USE_ITEM_REWARD,
    MAKER_SKILL,
    USE_REMOTE,
    ADMIN_CHAT,
    PARTYCHAT,
    WHISPER,
    MESSENGER,
    PLAYER_INTERACTION,
    PARTY_OPERATION,
    DENY_PARTY_REQUEST,
    GUILD_OPERATION,
    DENY_GUILD_REQUEST,
    ADMIN_COMMAND,
    ADMIN_LOG,
    BUDDYLIST_MODIFY,
    NOTE_ACTION,
    USE_DOOR,
    CHANGE_KEYMAP,
    RPS_ACTION,
    RING_ACTION,
    WEDDING_ACTION,
    OPEN_FAMILY,
    ADD_FAMILY,
    ACCEPT_FAMILY,
    USE_FAMILY,
    BBS_OPERATION,
    ENTER_MTS,
    USE_SOLOMON_ITEM,
    USE_GACHA_EXP,
    CLICK_GUIDE,
    ARAN_COMBO_COUNTER,
    MOVE_PET,
    PET_CHAT,
    PET_COMMAND,
    PET_LOOT,
    PET_AUTO_POT,
    PET_EXCLUDE_ITEMS,
    MOVE_SUMMON,
    SUMMON_ATTACK,
    DAMAGE_SUMMON,
    BEHOLDER,
    MOVE_LIFE,
    AUTO_AGGRO,
    MOB_DAMAGE_MOB_FRIENDLY,
    MONSTER_BOMB,
    MOB_DAMAGE_MOB,
    NPC_ACTION,
    ITEM_PICKUP,
    DAMAGE_REACTOR,
    TOUCHING_REACTOR,
    TEMP_SKILL,
    MTV,
    SNOWBALL,
    LEFT_KNOCKBACK,
    COCONUT,
    MATCH_TABLE,
    MONSTER_CARNIVAL,
    PARTY_SEARCH_REGISTER,
    PARTY_SEARCH_START,
    PLAYER_UPDATE,
    CHECK_CASH,
    REQUEST_PARAM,
    CASHSHOP_OPERATION,
    OPEN_ITEMUI,
    CLOSE_ITEMUI,
    USE_ITEMU,
    DETECT_ERROR,
    MTS_OPERATION,
    USE_HAMMER;
    //0x4F = 패밀리창
    // 0x49 = 가계도
    // 0xbe = move player
    // 0xE5 = npc_talk
    // 0x56 = cashshop
    // 0x2e = buddy
    // 0x95 부턴가 전부다 몬스터관련
    // 0x50 = charINFO
    // 0x6e = spawn_pla
    // 0x55 - ingame
    // 0x14 - 수락하기 눌럿을때 뜸
    // 0x12 - 수락하기 눌럿을때 뜸
    // 0x97 - 수락하기 눌럿을때 뜸
    // 0x14 = update_stats
    // 0x19 = update_skill
    // 0xA7 = skill_effect
    // 0x9F = SEND_HINT
    var value = -2
    val checkState = true

    companion object : KLogging() {
        var loaded = false

        init { loadOpcode() }

        fun loadOpcode() {
            if (!loaded) {
                try {
                    val prop = OpcodeProperties("recvops.properties")
                    values().forEach {
                        val code = prop.getString(it.name)?.toInt() ?: -2
                        if (code == -2) {
                            logger.debug { "${it.name} is not loaded correctly." }
                        } else {
                            logger.trace { "$it - $code is loaded correctly." }
                            it.value = code
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error caused when trying to load recvops.properties." }
                } finally {
                    loaded = true
                }
            }
        }

        fun reloadOpcode() {
            loaded = false
            loadOpcode()
        }

        fun valueOf(value: Int) = values().find { it.value == value }
    }
}