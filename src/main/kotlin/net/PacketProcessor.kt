package net

import client.DCHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import net.RecvPacketOpcode.*
import net.server.channel.handlers.*
import net.server.handlers.CustomPacketHandler
import net.server.handlers.KeepAliveHandler
import net.server.handlers.LoginRequiringNoOpHandler
import net.server.handlers.login.*

/**
 * Manages the registration and retrieval of packet handlers for processing incoming client packets.
 *
 * This class maintains an array of [PacketHandler] instances indexed by packet opcode,
 * allowing efficient lookup of the appropriate handler for each incoming packet type.
 * Different handlers are registered depending on whether the processor is used for
 * the login server or a game channel server.
 */
class PacketProcessor {
    /**
     * Array of packet handlers indexed by packet opcode value.
     * Each index corresponds to a [RecvPacketOpcode] value, and the element
     * at that index is the handler responsible for processing that packet type.
     */
    val handlers = arrayOfNulls<PacketHandler>(RecvPacketOpcode.values().maxOf { it.value } + 1)

    /**
     * Retrieves the packet handler for the specified packet opcode.
     *
     * @param packetId The opcode of the packet to find a handler for
     * @return The [PacketHandler] registered for the given opcode, or `null` if
     *         no handler is registered or the opcode is out of bounds
     */
    fun getHandler(packetId: Short): PacketHandler? {
        if (packetId > handlers.size) return null
        return handlers[packetId.toInt()]
    }

    /**
     * Registers a packet handler for a specific opcode.
     *
     * @param code The receive packet opcode to register the handler for
     * @param handler The handler that will process packets with the given opcode
     */
    private fun registerHandler(code: RecvPacketOpcode, handler: PacketHandler) {
        try {
            handlers[code.value] = handler
        } catch (e: ArrayIndexOutOfBoundsException) {
            //log("Index Out of Bounds caused during when registering handler. Code: ${code}, Value: ${code.value}", javaClass.simpleName, Logger.Type.WARNING, e.stackTrace)
        }
    }

    /**
     * Initializes and registers all packet handlers based on the channel type.
     *
     * For login servers (channel < 0), registers handlers for authentication,
     * character selection, and character creation packets.
     *
     * For game channel servers (channel >= 0), registers handlers for gameplay
     * packets including movement, combat, items, social features, and more.
     *
     * @param channel The channel number. A negative value indicates a login server,
     *                while zero or positive values indicate a game channel server.
     */
    fun reset(channel: Int) {
        registerHandler(PONG, KeepAliveHandler())
        registerHandler(CUSTOM_PACKET, CustomPacketHandler())
        if (channel < 0) { //login
            registerHandler(LOGIN_SUCCESS, LoginSuccessHandler())
            registerHandler(CLIENT_CHECK, ClientCheckHandler())
            registerHandler(CHARLIST_REQUEST, CharListRequestHandler())
            registerHandler(CHAR_SELECT, CharSelectedHandler())
            registerHandler(LOGIN_PASSWORD, LoginPasswordHandler())
            registerHandler(RELOG, ReLogRequestHandler())
            registerHandler(CHECK_CHAR_NAME, CheckCharNameHandler())
            registerHandler(CREATE_CHAR, CreateCharHandler())
            registerHandler(DELETE_CHAR, DeleteCharHandler())
        } else {
            //CHANNEL HANDLERS
            registerHandler(DETECT_ERROR, DCHandler())
            registerHandler(CHANGE_CHANNEL, ChangeChannelHandler())
            registerHandler(STRANGE_DATA, LoginRequiringNoOpHandler)
            registerHandler(GENERAL_CHAT, GeneralChatHandler())
            registerHandler(WHISPER, WhisperHandler())
            registerHandler(NPC_TALK, NPCTalkHandler())
            registerHandler(NPC_TALK_MORE, NPCMoreTalkHandler())
            registerHandler(QUEST_ACTION, QuestActionHandler())
            registerHandler(NPC_SHOP, NPCShopHandler())
            registerHandler(ITEM_SORT, ItemSortHandler())
            registerHandler(ITEM_MOVE, ItemMoveHandler())
            registerHandler(MESO_DROP, MesoDropHandler())
            registerHandler(PLAYER_LOGGEDIN, PlayerLoggedInHandler())
            registerHandler(CHANGE_MAP, ChangeMapHandler())
            registerHandler(MOVE_LIFE, MoveLifeHandler())
            registerHandler(CLOSE_RANGE_ATTACK, CloseRangeDamageHandler())
            registerHandler(RANGED_ATTACK, RangedAttackHandler())
            registerHandler(MAGIC_ATTACK, MagicDamageHandler())
            registerHandler(TAKE_DAMAGE, TakeDamageHandler())
            registerHandler(MOVE_PLAYER, MovePlayerHandler())
            registerHandler(USE_CASH_ITEM, UseCashItemHandler())
            registerHandler(USE_ITEM, UseItemHandler())
            registerHandler(USE_RETURN_SCROLL, UseItemHandler())
            registerHandler(USE_UPGRADE_SCROLL, ScrollHandler())
            registerHandler(USE_SUMMON_BAG, UseSummonBag())
            registerHandler(FACE_EXPRESSION, FaceExpressionHandler())
            registerHandler(HEAL_OVER_TIME, HealOvertimeHandler())
            registerHandler(ITEM_PICKUP, ItemPickupHandler())
            registerHandler(CHAR_INFO_REQUEST, CharInfoRequestHandler())
            registerHandler(SPECIAL_MOVE, SpecialMoveHandler())
            registerHandler(USE_INNER_PORTAL, InnerPortalHandler())
            registerHandler(CANCEL_BUFF, CancelBuffHandler())
            registerHandler(CANCEL_ITEM_EFFECT, CancelItemEffectHandler())
            registerHandler(PLAYER_INTERACTION, PlayerInteractionHandler())
            registerHandler(DISTRIBUTE_AP, DistributeAPHandler())
            registerHandler(DISTRIBUTE_SP, DistributeSPHandler())
            registerHandler(CHANGE_KEYMAP, KeymapChangeHandler())
            registerHandler(CHANGE_MAP_SPECIAL, ChangeMapSpecialHandler())
            registerHandler(STORAGE, StorageHandler())
            registerHandler(GIVE_FAME, GiveFameHandler())
            registerHandler(PARTY_OPERATION, PartyOperationHandler())
            registerHandler(DENY_PARTY_REQUEST, DenyPartyRequestHandler())
            registerHandler(PARTYCHAT, PartyChatHandler())
            registerHandler(USE_DOOR, DoorHandler())
            //registerHandler(ENTER_MTS, EnterMTSHandler())
            registerHandler(ENTER_CASHSHOP, EnterCashShopHandler())
            registerHandler(DAMAGE_SUMMON, DamageSummonHandler())
            registerHandler(MOVE_SUMMON, MoveSummonHandler())
            registerHandler(SUMMON_ATTACK, SummonDamageHandler())
            registerHandler(BUDDYLIST_MODIFY, BuddyListModifyHandler())
            registerHandler(USE_ITEMEFFECT, UseItemEffectHandler())
            registerHandler(USE_CHAIR, UseChairHandler())
            registerHandler(CANCEL_CHAIR, CancelChairHandler())
            registerHandler(DAMAGE_REACTOR, ReactorHitHandler())
            registerHandler(GUILD_OPERATION, GuildOperationHandler())
            registerHandler(DENY_GUILD_REQUEST, DenyGuildRequestHandler())
            registerHandler(BBS_OPERATION, BBSOperationHandler())
            registerHandler(SKILL_EFFECT, SkillEffectHandler())
            registerHandler(MESSENGER, MessengerHandler())
            registerHandler(NPC_ACTION, NPCAnimation())
            registerHandler(CHECK_CASH, TouchingCashShopHandler())
            registerHandler(REQUEST_PARAM, TouchingCashShopHandler())
            registerHandler(CASHSHOP_OPERATION, CashOperationHandler())
            registerHandler(COUPON_CODE, CouponCodeHandler())
            registerHandler(SPAWN_PET, SpawnPetHandler())
            registerHandler(MOVE_PET, MovePetHandler())
            registerHandler(PET_CHAT, PetChatHandler())
            registerHandler(PET_COMMAND, PetCommandHandler())
            registerHandler(PET_FOOD, PetFoodHandler())
            registerHandler(PET_LOOT, PetLootHandler())
            registerHandler(AUTO_AGGRO, AutoAggroHandler())
            registerHandler(MONSTER_BOMB, MonsterBombHandler())
            registerHandler(CANCEL_DEBUFF, CancelDeBuffHandler())
            //registerHandler(RecvPacketOpcode.USE_SKILL_BOOK, new SkillBookHandler());
            //registerHandler(RecvPacketOpcode.SKILL_MACRO, new SkillMacroHandler());
            registerHandler(NOTE_ACTION, NoteActionHandler())
            //registerHandler(RecvPacketOpcode.CLOSE_CHALKBOARD, new CloseChalkboardHandler());
            registerHandler(USE_MOUNT_FOOD, UseMountFoodHandler())
            //registerHandler(MTS_OPERATION, MTSHandler())
            //registerHandler(RING_ACTION, RingActionHandler())
            //registerHandler(RecvPacketOpcode.SPOUSE_CHAT, new SpouseChatHandler());
            registerHandler(PET_AUTO_POT, PetAutoPotHandler())
            registerHandler(PET_EXCLUDE_ITEMS, PetExcludeItemsHandler())
            registerHandler(TOUCH_MONSTER_ATTACK, TouchMonsterDamageHandler())
            registerHandler(TROCK_ADD_MAP, TrockAddMapHandler())
            registerHandler(HIRED_MERCHANT_REQUEST, HiredMerchantRequest())
            registerHandler(MOB_DAMAGE_MOB, MobDamageMobHandler())
            registerHandler(REPORT, ReportHandler())
            //registerHandler(RecvPacketOpcode.MONSTER_BOOK_COVER, new MonsterBookCoverHandler());
            //registerHandler(RecvPacketOpcode.AUTO_DISTRIBUTE_AP, new AutoAssignHandler());
            //registerHandler(RecvPacketOpcode.MAKER_SKILL, new MakerSkillHandler());
            //registerHandler(RecvPacketOpcode.ADD_FAMILY, new FamilyAddHandler());
            //registerHandler(RecvPacketOpcode.USE_FAMILY, new FamilyUseHandler());
            //registerHandler(RecvPacketOpcode.USE_HAMMER, new UseHammerHandler());
            registerHandler(SCRIPTED_ITEM, ScriptedItemHandler())
            registerHandler(TOUCHING_REACTOR, TouchReactorHandler())
            //registerHandler(RecvPacketOpcode.BEHOLDER, new BeholderHandler());
            //registerHandler(ADMIN_COMMAND, AdminCommandHandler())
            //registerHandler(ADMIN_LOG, AdminLogHandler())
            //registerHandler(RecvPacketOpcode.USE_SOLOMON_ITEM, new UseSolomonHandler());
            registerHandler(USE_ITEM_REWARD, ItemRewardHandler())
            registerHandler(USE_REMOTE, RemoteGachaponHandler())
            //registerHandler(RecvPacketOpcode.ACCEPT_FAMILY, new AcceptFamilyHandler());
            registerHandler(DUEY_ACTION, DueyHandler())
            registerHandler(USE_DEATHITEM, UseDeathItemHandler())
            //registerHandler(RecvOpcode.PLAYER_UPDATE, new PlayerUpdateHandler());don't use unused stuff
            //registerHandler(USE_MLIFE, UseLifeHandler())
            //registerHandler(RecvPacketOpcode.USE_CATCH_ITEM, new UseCatchItemHandler());
            registerHandler(MOB_DAMAGE_MOB_FRIENDLY, MobDamageMobFriendlyHandler())
            //registerHandler(RecvPacketOpcode.PARTY_SEARCH_REGISTER, new PartySearchRegisterHandler());
            //registerHandler(RecvPacketOpcode.PARTY_SEARCH_START, new PartySearchStartHandler());
            registerHandler(ITEM_SORT2, ItemIdSortHandler())
            registerHandler(LEFT_KNOCKBACK, LeftKnockbackHandler())
            //registerHandler(RecvPacketOpcode.SNOWBALL, new SnowballHandler());
            registerHandler(COCONUT, CoconutHandler())
            registerHandler(CLICK_GUIDE, ClickGuideHandler())
            registerHandler(FREDRICK_ACTION, FredrickHandler())
            //registerHandler(RecvPacketOpcode.MONSTER_CARNIVAL, new MonsterCarnivalHandler());
            registerHandler(REMOTE_STORE, RemoteStoreHandler())
            //registerHandler(RecvPacketOpcode.WEDDING_ACTION, new WeddingHandler());
            registerHandler(ADMIN_CHAT, AdminChatHandler())
        }
    }

    companion object {
        private val instances = mutableMapOf<String, PacketProcessor>()

        /**
         * Retrieves or creates a [PacketProcessor] instance for the specified world and channel.
         *
         * Processor instances are cached and reused based on the world-channel combination.
         * If no processor exists for the given combination, a new one is created and initialized
         * with the appropriate handlers for that channel type.
         *
         * @param world The world ID
         * @param channel The channel ID. A negative value indicates a login server.
         * @return The [PacketProcessor] instance for the specified world and channel
         */
        @Synchronized fun getProcessor(world: Int, channel: Int): PacketProcessor {
            val worldChannelPair = "$world$channel"
            var processor = instances[worldChannelPair]
            if (processor == null) {
                processor = PacketProcessor()
                processor.reset(channel)
                instances[worldChannelPair] = processor
            }
            return processor
        }
    }
}