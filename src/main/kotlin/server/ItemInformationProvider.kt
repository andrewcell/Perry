package server

import client.Character
import client.Client
import client.GameJob
import client.SkillFactory
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.Item
import client.inventory.WeaponType
import com.beust.klaxon.Klaxon
import constants.ItemConstants
import mu.KLoggable
import provider.Data
import provider.DataProviderFactory
import provider.DataTool
import tools.ResourceFile
import tools.ServerJSON.settings
import tools.settings.MonsterCardDataDatabase
import java.io.File
import java.sql.SQLException
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

object ItemInformationProvider : KLoggable {
    override val logger = logger()
    private val itemData = DataProviderFactory.getDataProvider(File(settings.wzPath + "/Item.wz"))
    private val equipData = DataProviderFactory.getDataProvider(File(settings.wzPath + "/Character.wz"))
    private val stringData = DataProviderFactory.getDataProvider(File(settings.wzPath + "/String.wz"))
    private val cashStringData = stringData.getData("Item.img")?.getChildByPath("Cash")
    private val consumeStringData = stringData.getData("Item.img")?.getChildByPath("Con")
    private val eqpStringData = stringData.getData("Item.img")?.getChildByPath("Eqp")
    private val etcStringData = stringData.getData("Item.img")?.getChildByPath("Etc")
    private val insStringData = stringData.getData("Item.img")?.getChildByPath("Ins")
    private val petStringData = stringData.getData("Item.img")?.getChildByPath("Pet")
    private val monsterBookId = loadMonsterCardIdData()
    private var inventoryTypeCache = mutableMapOf<Int, InventoryType>()
    private var slotMaxCache = mutableMapOf<Int, Short>()
    private var itemEffects = mutableMapOf<Int, StatEffect>()
    private var equipStatsCache = mutableMapOf<Int, Map<String, Int>>()
    private var equipCache = mutableMapOf<Int, Equip>()
    private var priceCache = mutableMapOf<Int, Double>()
    private var wholePriceCache = mutableMapOf<Int, Int>()
    private var projectileWatkCache = mutableMapOf<Int, Int>()
    private var nameCache = mutableMapOf<Int, String?>()
    private var msgCache = mutableMapOf<Int, String?>()
    private var dropRestrictionCache = mutableMapOf<Int, Boolean>()
    private var pickupRestrictionCache = mutableMapOf<Int, Boolean>()
    private var getMesoCache = mutableMapOf<Int, Int>()
    private var onEquipUnTradableCache = mutableMapOf<Int, Boolean>()
    private var scriptedItemCache = mutableMapOf<Int, ScriptedItem>()
    private var karmaCache = mutableMapOf<Int, Boolean>()
    private var triggerItemCache = mutableMapOf<Int, Int>()
    private var expCache = mutableMapOf<Int, Int>()
    private var levelCache = mutableMapOf<Int, Int>()
    private var rewardCache = mutableMapOf<Int, Pair<Int, List<RewardItem>>>()
    private var itemNameCache: List<Pair<Int, String>> = mutableListOf()
    private var consumeOnPickupCache = mutableMapOf<Int, Boolean>()
    private var isQuestItemCache = mutableMapOf<Int, Boolean>()

    fun getInventoryType(itemId: Int): InventoryType {
        inventoryTypeCache[itemId]?.let { return it }
        var ret: InventoryType
        val idString = "0$itemId"
        var root = itemData.root
        root.subDirectories.forEach { topDir ->
            topDir.files.forEach { iFile ->
                if (iFile.name == "${idString.substring(0, 4)}.img") {
                    ret = InventoryType.getByWzName(topDir.name)
                    inventoryTypeCache[itemId] = ret
                    return ret
                } else if (iFile.name == "${idString.substring(1)}.img") {
                    ret = InventoryType.getByWzName(topDir.name)
                    inventoryTypeCache[itemId] = ret
                    return ret
                }
            }
        }
        root = equipData.root
        root.subDirectories.forEach { topDir ->
            topDir.files.forEach { iFile ->
                if (iFile.name == "$idString.img") {
                    ret = InventoryType.EQUIP
                    inventoryTypeCache[itemId] = ret
                    return ret
                }
            }
        }
        ret = InventoryType.UNDEFINED
        inventoryTypeCache[itemId] = ret
        return ret
    }

    private fun traceWz(message: String) = logger.trace { "Entering Item.wz - $message.img"}

    fun getAllItems(): List<Pair<Int, String>> {
        if (itemNameCache.isNotEmpty()) return itemNameCache
        val itemPairs = mutableListOf<Pair<Int, String>>()
        var itemsData = cashStringData

        itemsData?.children?.forEach { itemFolder ->
            itemPairs.add(Pair(itemFolder.name.toInt(), DataTool.getString("name", itemFolder, "NO-NAME")))
        }
        itemsData = consumeStringData
        traceWz(itemsData?.name.toString())
        itemsData?.children?.forEach { itemFolder ->
            itemPairs.add(Pair(itemFolder.name.toInt(), DataTool.getString("name", itemFolder, "NO-NAME")))
        }
        itemsData = eqpStringData
        traceWz(itemsData?.name.toString())
        itemsData?.children?.forEach { eqpType ->
            eqpType.children.forEach { itemFolder ->
                val name = DataTool.getString("name", itemFolder, "NO-NAME")
                val itemId = itemFolder.name.toInt()
                itemPairs.add(Pair(itemId, name))
                logger.trace { "Item: Eqp.img - ${eqpType.name}.img - $itemId - $name" }
            }
        }
        itemsData = etcStringData
        traceWz(itemsData?.name.toString())
        itemsData?.children?.forEach { itemFolder ->
            val name = DataTool.getString("name", itemFolder, "NO-NAME")
            val itemId = itemFolder.name.toInt()
            itemPairs.add(Pair(itemId, name))
            logger.trace { "Item: Etc.img - $itemId - $name" }
        }
        itemsData = insStringData
        traceWz(itemsData?.name.toString())
        itemsData?.children?.forEach { itemFolder ->
            val name = DataTool.getString("name", itemFolder, "NO-NAME")
            val itemId = itemFolder.name.toInt()
            itemPairs.add(Pair(itemId, name))
            logger.trace { "Item: Ins.img - $itemId - $name" }
        }
        itemsData = petStringData
        traceWz(itemsData?.name.toString())
        itemsData?.children?.forEach { itemFolder ->
            val name = DataTool.getString("name", itemFolder, "NO-NAME")
            val itemId = itemFolder.name.toInt()
            itemPairs.add(Pair(itemId, name))
            logger.trace { "Item: Pet.img - $itemId - $name" }
        }
        return itemPairs
    }

    private fun getStringData(itemId: Int): Data? {
        var cat = ""
        val data: Data?
        when {
            (itemId >= 5010000) -> data = cashStringData
            (itemId in 2000000..2999999) -> data = consumeStringData
            ((itemId in 1010000..1039999) || (itemId in 1122000..1122999) || (itemId in 1142000..1142999)) -> {
                data = eqpStringData
                cat = "Accessory"
            }
            (itemId in 1000000..1009999) -> {
                data = eqpStringData
                cat = "Cap"
            }
            (itemId in 1102000..1102999) -> {
                data = eqpStringData
                cat = "Cape"
            }
            (itemId in 1040000..1049999) -> {
                data = eqpStringData
                cat = "Coat"
            }
            (itemId in 20000..21999) -> {
                data = eqpStringData
                cat = "Face"
            }
            (itemId in 1080000..1089999) -> {
                data = eqpStringData
                cat = "Glove"
            }
            (itemId in 30000..31999) -> {
                data = eqpStringData
                cat = "Hair"
            }
            (itemId in 1050000..1059999) -> {
                data = eqpStringData
                cat = "Longcoat"
            }
            (itemId in 1060000..1069999) -> {
                data = eqpStringData
                cat = "Pants"
            }
            (itemId in 1802000..1809999) -> {
                data = eqpStringData
                cat = "PetEquip"
            }
            (itemId in 1112000..1119999) -> {
                data = eqpStringData
                cat = "Ring"
            }
            (itemId in 1092000..1099999) -> {
                data = eqpStringData
                cat = "Shield"
            }
            (itemId in 1070000..1079999) -> {
                data = eqpStringData
                cat = "Shoes"
            }
            (itemId in 1900000..1999999) -> {
                data = eqpStringData
                cat = "Taming"
            }
            (itemId in 1300000..1799999) -> {
                data = eqpStringData
                cat = "Weapon"
            }
            (itemId in 4000000..4999999) -> data = etcStringData
            (itemId in 3000000..3999999) -> data = insStringData
            (itemId in 5000000..5009999) -> data = petStringData
            else -> return null
        }
        return if (cat == "") {
            data?.getChildByPath(itemId.toString())
        } else {
            data?.getChildByPath("$cat/$itemId")
        }
    }

    private fun getItemData(itemId: Int): Data? {
        var ret: Data?
        val idString = "0$itemId"
        var root = itemData.root
        root.subDirectories.forEach { topDir ->
            topDir.files.forEach { iFile ->
                if (iFile.name == "${idString.substring(0, 4)}.img") {
                    ret = itemData.getData("${topDir.name}/${iFile.name}") ?: return null
                    ret = ret?.getChildByPath(idString)
                    return ret
                } else if (iFile.name == "${idString.substring(1)}.img") {
                    return itemData.getData("${topDir.name}/${iFile.name}")
                }
            }
        }
        root = equipData.root
        root.subDirectories.forEach { topDir ->
            topDir.files.forEach { iFile ->
                if (iFile.name == "$idString.img") {
                    return equipData.getData("${topDir.name}/${iFile.name}")
                }
            }
        }
        return null
    }

    fun getSlotMax(c: Client, itemId: Int): Short {
        slotMaxCache[itemId]?.let { return it }
        var ret = 0
        val item = getItemData(itemId)
        if (item != null) {
            val smEntry = item.getChildByPath("info/slotMax")
            if (smEntry == null) {
                ret = if (getInventoryType(itemId).type == InventoryType.EQUIP.type) {
                    1
                } else {
                    100
                }
            } else {
                ret = DataTool.getInt(smEntry)
                ret += if (ItemConstants.isThrowingStar(itemId)) {
                    (SkillFactory.getSkill(4100000)?.let { c.player?.getSkillLevel(it) } ?: 1) * 10
                } else {
                    (SkillFactory.getSkill(5200000)?.let { c.player?.getSkillLevel(it) } ?: 1) * 10
                }
            }
        }
        if (!ItemConstants.isRechargeable(itemId)) {
            slotMaxCache[itemId] = ret.toShort()
        }
        return ret.toShort()
    }

    fun getMeso(itemId: Int): Int {
        getMesoCache[itemId]?.let { return it }
        val item = getItemData(itemId) ?: return -1
        val pData = item.getChildByPath("info/meso") ?: return -1
        val pEntry = DataTool.getInt(pData)
        getMesoCache[itemId] = pEntry
        return pEntry
    }

    fun getWholePrice(itemId: Int): Int {
        wholePriceCache[itemId]?.let { return it }
        val item = getItemData(itemId) ?: return -1
        val pData = item.getChildByPath("info/price") ?: return -1
        val pEntry = DataTool.getInt(pData)
        wholePriceCache[itemId] = pEntry
        return pEntry
    }

    fun getPrice(itemId: Int): Double {
        priceCache[itemId]?.let { return it }
        val item = getItemData(itemId) ?: return -1.0
        var pData = item.getChildByPath("info/unitPrice")
        val pEntry: Double
        if (pData != null) {
            pEntry = try {
                DataTool.getDouble(pData) ?: -1.0
            } catch (e: Exception) {
                DataTool.getInt(pData).toDouble()
            }
        } else {
            pData = item.getChildByPath("info/price") ?: return -1.0
            pEntry = DataTool.getInt(pData).toDouble()
        }
        priceCache[itemId] = pEntry
        return pEntry
    }

    private fun getEquipStats(itemId: Int): Map<String, Int>? {
        equipStatsCache[itemId]?.let { return it }
        val ret = mutableMapOf<String, Int>()
        val item = getItemData(itemId) ?: return null
        val info = item.getChildByPath("info") ?: return null
        info.children.forEach { data ->
            if (data.name.startsWith("inc")) {
                ret[data.name.substring(3)] = DataTool.getIntConvert(data) ?: 0
            }
        }
        ret["reqJob"] = DataTool.getInt("reqJob", info, 0)
        ret["reqLevel"] = DataTool.getInt("reqLevel", info, 0)
        ret["reqDEX"] = DataTool.getInt("reqDEX", info, 0)
        ret["reqSTR"] = DataTool.getInt("reqSTR", info, 0)
        ret["reqINT"] = DataTool.getInt("reqINT", info, 0)
        ret["reqLUK"] = DataTool.getInt("reqLUK", info, 0)
        ret["reqPOP"] = DataTool.getInt("reqPOP", info, 0)
        ret["cash"] = DataTool.getInt("cash", info, 0)
        ret["tuc"] = DataTool.getInt("tuc", info, 0)
        ret["cursed"] = DataTool.getInt("cursed", info, 0)
        ret["success"] = DataTool.getInt("success", info, 0)
        ret["fs"] = DataTool.getInt("fs", info, 0)
        equipStatsCache[itemId] = ret
        return ret
    }

    fun getScrollReqs(itemId: Int): List<Int> {
        val ret = mutableListOf<Int>()
        val data = getItemData(itemId)?.getChildByPath("req") ?: return emptyList()
        data.children.forEach {
            ret.add(DataTool.getInt(it))
        }
        return ret
    }

    fun getWeaponType(itemId: Int): WeaponType {
        val cat = (itemId / 10000) % 100
        val type = arrayOf(
            WeaponType.SWORD1H,
            WeaponType.AXE1H,
            WeaponType.BLUNT1H,
            WeaponType.DAGGER,
            WeaponType.NOT_A_WEAPON,
            WeaponType.NOT_A_WEAPON,
            WeaponType.NOT_A_WEAPON,
            WeaponType.WAND,
            WeaponType.STAFF,
            WeaponType.NOT_A_WEAPON,
            WeaponType.SWORD2H,
            WeaponType.AXE2H,
            WeaponType.BLUNT2H,
            WeaponType.SPEAR,
            WeaponType.POLE_ARM,
            WeaponType.BOW,
            WeaponType.CROSSBOW,
            WeaponType.CLAW,
            WeaponType.KNUCKLE,
            WeaponType.GUN
        )
        return if (cat < 30 || cat > 49) return WeaponType.NOT_A_WEAPON else type[cat - 30]
    }

    fun scrollEquipWithId(equip: Item, scrollId: Int, usingWhiteScroll: Boolean, isGM: Boolean): Item? {
        if (equip is Equip) {
            val stats = getEquipStats(scrollId)
            val eqStats = getEquipStats(equip.itemId)
            val success = if (isGM) 100 else stats?.getOrDefault("success", 0) ?: 0
            if (((equip.upgradeSlots > 0 || isCleanSlate(scrollId)) && ceil(Math.random() * 100.0) <= success)) {
                val flag = equip.flag
                when (scrollId) {
                    2040727 -> {
                        flag.or(ItemConstants.SPIKES)
                        equip.flag = flag
                        return equip
                    }
                    2041058 -> {
                        flag.or(ItemConstants.COLD)
                        equip.flag = flag
                        return equip
                    }
                    2049000, 2049001, 2049002, 2049003 -> {
                        if (equip.level + equip.upgradeSlots < eqStats!!.getOrDefault("tuc", 9999)) {
                            equip.upgradeSlots++
                        }
                    }
                    2049100, 2049101, 2049102 -> {
                        var inc = 1
                        if (Random.nextInt(2) == 0) inc = -1
                        with(equip) {
                            if (str > 0) str = (str + (Random.nextInt(6) * inc)).toShort()
                            if (dex > 0) dex = (dex + (Random.nextInt(6) * inc)).toShort()
                            if (int > 0) int = (int + (Random.nextInt(6) * inc)).toShort()
                            if (luk > 0) luk = (luk + (Random.nextInt(6) * inc)).toShort()
                            if (watk > 0) watk = (watk + (Random.nextInt(6) * inc)).toShort()
                            if (wdef > 0) wdef = (wdef + (Random.nextInt(6) * inc)).toShort()
                            if (matk > 0) matk = (matk + (Random.nextInt(6) * inc)).toShort()
                            if (mdef > 0) mdef = (mdef + (Random.nextInt(6) * inc)).toShort()
                            if (acc > 0) acc = (acc + (Random.nextInt(6) * inc)).toShort()
                            if (avoid > 0) avoid = (avoid + (Random.nextInt(6) * inc)).toShort()
                            if (speed > 0) speed = (speed + (Random.nextInt(6) * inc)).toShort()
                            if (jump > 0) jump = (jump + (Random.nextInt(6) * inc)).toShort()
                            if (hp > 0) hp = (hp + (Random.nextInt(6) * inc)).toShort()
                            if (mp > 0) mp = (mp + (Random.nextInt(6) * inc)).toShort()
                        }
                    }
                    else -> {
                        stats?.entries?.forEach {
                            with (equip) {
                                when (it.key) {
                                    "STR" -> str = (str + it.value).toShort()
                                    "DEX" -> dex = (dex + it.value).toShort()
                                    "INT" -> int = (int + it.value).toShort()
                                    "LUK" -> luk = (luk + it.value).toShort()
                                    "PAD" -> watk = (watk + it.value).toShort()
                                    "PDD" -> wdef = (wdef + it.value).toShort()
                                    "MAD" -> matk = (matk + it.value).toShort()
                                    "MDD" -> mdef = (mdef + it.value).toShort()
                                    "ACC" -> acc = (acc + it.value).toShort()
                                    "EVA" -> avoid = (avoid + it.value).toShort()
                                    "Speed" -> speed = (speed + it.value).toShort()
                                    "Jump" -> jump = (jump + it.value).toShort()
                                    "MHP" -> hp = (hp + it.value).toShort()
                                    "MMP" -> mp = (mp+ it.value).toShort()
                                }
                            }
                        }
                    }
                }
                if (!isCleanSlate(scrollId)) {
                    with (equip) {
                        upgradeSlots = (upgradeSlots - 1).toByte()
                        level = (level + 1).toByte()
                    }
                }
            } else {
                if (!usingWhiteScroll && isCleanSlate(scrollId)) {
                    equip.upgradeSlots = (equip.upgradeSlots - 1).toByte()
                }
                if (Random.nextInt(101) < (stats?.get("cursed") ?: 111)) return null
            }
        }
        return equip
    }

    fun getEquipById(equipId: Int): Item {
        val equip = Equip(equipId, 0, -1)
        equip.quantity = 1
        val stats = getEquipStats(equipId)
        stats?.entries?.forEach {
            with (equip) {
                when (it.key) {
                    "STR" -> str = it.value.toShort()
                    "DEX" -> dex = it.value.toShort()
                    "INT" -> int = it.value.toShort()
                    "LUK" -> luk = it.value.toShort()
                    "PAD" -> watk = it.value.toShort()
                    "PDD" -> wdef = it.value.toShort()
                    "MAD" -> matk = it.value.toShort()
                    "MDD" -> mdef = it.value.toShort()
                    "ACC" -> acc = it.value.toShort()
                    "EVA" -> avoid = it.value.toShort()
                    "Speed" -> speed = it.value.toShort()
                    "Jump" -> jump = it.value.toShort()
                    "MHP" -> hp = it.value.toShort()
                    "MMP" -> mp = it.value.toShort()
                    "tuc" -> upgradeSlots = it.value.toByte()
                }
            }
        }
        if (isDropRestricted(equipId)) {
            val flag = equip.flag.or(ItemConstants.UNTRADEABLE)
            equip.flag = flag
        }
        if ((stats?.get("fs") ?: 0) > 0) {
            val flag = equip.flag.or(ItemConstants.SPIKES)
            equip.flag = flag
            equipCache[equipId] = equip
        }
        return equip.copy()
    }

    fun isUnTradeableOnEquip(itemId: Int): Boolean {
        onEquipUnTradableCache[itemId]?.let { return it }
        val unTradableOnEquip = DataTool.getIntConvert("info/equipTradeBlock", getItemData(itemId), 0) > 0
        onEquipUnTradableCache[itemId] = unTradableOnEquip
        return unTradableOnEquip
    }

    fun noCancelMouse(itemId: Int): Boolean {
        val item = getItemData(itemId) ?: return false
        return DataTool.getIntConvert("info/noCancelMouse", item, 0) == 1
    }

    private fun isCleanSlate(scrollId: Int) = scrollId in 2049000..2049003

    private fun loadMonsterCardIdData(): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        try {
            val cardData = ResourceFile.load("MonsterCardData.json")
                ?.let { Klaxon().parseArray<MonsterCardDataDatabase>(it) } ?: return emptyMap()
            cardData.forEach {
                map[it.id] = it.cardId
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to get monster card id data from database." }
        }
        return map.toMap()
    }

    private fun getRandStat(defaultValue: Short, maxRange: Int): Short {
        if (defaultValue == 0.toShort()) return 0
        val max = ceil(defaultValue * 0.1).coerceAtMost(maxRange.toDouble())
        return ((defaultValue - max) + floor(Random.nextDouble() * (max * 2 + 1))).toInt().toShort()
    }

    fun randomizeStats(equip: Equip): Equip {
        with(equip) {
            str = getRandStat(str, 5)
            dex = getRandStat(dex, 5)
            int = getRandStat(int, 5)
            luk = getRandStat(luk, 5)
            matk = getRandStat(matk, 5)
            watk = getRandStat(watk, 5)
            acc = getRandStat(acc, 5)
            avoid = getRandStat(avoid, 5)
            jump = getRandStat(jump, 5)
            speed = getRandStat(speed, 5)
            wdef = getRandStat(wdef, 10)
            mdef = getRandStat(mdef, 10)
            hp = getRandStat(hp, 10)
            mp = getRandStat(mp, 10)
        }
        return equip
    }

    fun getItemEffect(itemId: Int): StatEffect? {
        var ret = itemEffects[itemId]
        if (ret == null) {
            val item = getItemData(itemId) ?: return null
            val spec = item.getChildByPath("spec")
            ret = StatEffect.loadItemEffectFromData(spec, itemId)
            itemEffects[itemId] = ret
        }
        return ret
    }

    fun getSummonMobs(itemId: Int): Array<IntArray> {
        val data = getItemData(itemId) ?: return arrayOf()
        val theInt = data.getChildByPath("mob")?.children?.size ?: 0
        val mobs2spawn = Array(theInt) { IntArray(2) }
        for (x in 0 until theInt) {
            mobs2spawn[x][0] = DataTool.getIntConvert("mob/$x/id", data)
            mobs2spawn[x][1] = DataTool.getIntConvert("mob/$x/prob", data)
        }
        return mobs2spawn
    }

    fun getWatkForProjectile(itemId: Int): Int {
        projectileWatkCache[itemId]?.let { return it }
        val data = getItemData(itemId)
        val atk = DataTool.getInt("info/incPAD", data, 0)
        projectileWatkCache[itemId] = atk
        return atk
    }

    fun getName(itemId: Int): String? {
        val strings = getStringData(itemId) ?: return null
        val ret = DataTool.getStringNullable("name", strings, null)
        nameCache[itemId] = ret
        return ret
    }

    fun getMsg(itemId: Int): String? {
        msgCache[itemId]?.let { return it }
        val strings = getStringData(itemId) ?: return null
        val ret = DataTool.getStringNullable("msg", strings, null)
        msgCache[itemId] = ret
        return ret
    }

    fun isDropRestricted(itemId: Int): Boolean {
        dropRestrictionCache[itemId]?.let { return it }
        val data = getItemData(itemId)
        var restricted = DataTool.getIntConvert("info/tradeBlock", data, 0) == 1
        if (!restricted) restricted = DataTool.getIntConvert("info/quest", data, 0) == 1
        dropRestrictionCache[itemId] = restricted
        return restricted
    }

    fun isPickupRestricted(itemId: Int): Boolean {
        pickupRestrictionCache[itemId]?.let { return it }
        val data = getItemData(itemId)
        val restricted = DataTool.getIntConvert("info/only", data, 0) == 1
        pickupRestrictionCache[itemId] = restricted
        return restricted
    }

    fun getSkillStats(itemId: Int, playerJob: Double): Map<String, Int>? {
        val ret = mutableMapOf<String, Int>()
        val item = getItemData(itemId) ?: return null
        val info = item.getChildByPath("info") ?: return null
        info.children.forEach {
            if (it.name.startsWith("inc")) {
                ret[it.name.substring(3)] = DataTool.getIntConvert(it) ?: 0
            }
        }
        ret["masterLevel"] = DataTool.getInt("masterLevel", info, 0)
        ret["reqSkillLevel"] = DataTool.getInt("reqSkillLevel", info, 0)
        ret["success"] = DataTool.getInt("success", info, 0)
        val skill = info.getChildByPath("skill")
        run loop@ {
            skill?.children?.forEachIndexed { index, _ ->
                val curSkill = DataTool.getInt(index.toString(), skill, 0)
                if (curSkill == 0) return@loop
                if (curSkill / 10000.0 == playerJob) {
                    ret["skillid"] = curSkill
                }
            }
            ret["skillid"]?.let { ret["skillid"] = 0 }
        }
        return ret
    }

    fun petsCanConsume(itemId: Int): List<Int> {
        val ret = mutableListOf<Int>()
        val data = getItemData(itemId) ?: return emptyList()
        data.children.forEachIndexed { index, _ ->
            val curPetId = DataTool.getInt("spec/$index", data, 0)
            if (curPetId == 0) return@forEachIndexed
            ret.add(curPetId)
        }
        return ret
    }

    fun isQuestItem(itemId: Int): Boolean {
        isQuestItemCache[itemId]?.let { return it }
        val data = getItemData(itemId) ?: return false
        val questItem = DataTool.getIntConvert("info/quest", data, 0) == 1
        isQuestItemCache[itemId] = questItem
        return questItem
    }

    fun getQuestIdFromItem(itemId: Int): Int {
        val data = getItemData(itemId)
        return DataTool.getIntConvert("info/quest", data, 0)
    }

    fun getCardMobId(id: Int) = monsterBookId[id]

    fun getScriptedItemInfo(itemId: Int): ScriptedItem? {
        scriptedItemCache[itemId]?.let { return it }
        if ((itemId / 10000) != 243) return null
        val script = ScriptedItem(
            DataTool.getInt("spec/npc", getItemData(itemId), 0),
            DataTool.getString("spec/script", getItemData(itemId), ""),
            DataTool.getInt("spec/runOnPickup", getItemData(itemId), 0) == 1
        )
        scriptedItemCache[itemId] = script
        return script
    }

    fun isKarmaAble(itemId: Int): Boolean {
        karmaCache[itemId]?.let{ return it }
        val restricted = DataTool.getIntConvert("info/tradeAvailable", getItemData(itemId), 0) > 0
        karmaCache[itemId] = restricted
        return restricted
    }

    fun isConsumeOnPickup(itemId: Int): Boolean {
        consumeOnPickupCache[itemId]?.let { return it }
        val data = getItemData(itemId)
        val consume = DataTool.getIntConvert("spec/consumeOnPickup", data, 0) == 1 || DataTool.getIntConvert("specEx/consumeOnPickup", data, 0) == 1
        consumeOnPickupCache[itemId] = consume
        return consume
    }

    fun isTwoHanded(itemId: Int): Boolean = when (getWeaponType(itemId)) {
        WeaponType.AXE2H, WeaponType.BLUNT2H, WeaponType.BOW, WeaponType.CLAW,
        WeaponType.CROSSBOW, WeaponType.POLE_ARM, WeaponType.SPEAR, WeaponType.SWORD2H,
        WeaponType.GUN, WeaponType.KNUCKLE -> true
        else -> false
    }

    fun isCash(itemId: Int) = itemId / 1000000 == 5 || getEquipStats(itemId)?.get("cash") == 1

    fun getStateChangeItem(itemId: Int): Int {
        triggerItemCache[itemId]?.let { return it }
        val triggerItem = DataTool.getIntConvert("info/stateChangeItem", getItemData(itemId), 0)
        triggerItemCache[itemId] = triggerItem
        return triggerItem
    }

    fun getExpById(itemId: Int): Int {
        expCache[itemId]?.let { return it }
        val exp = DataTool.getIntConvert("spec/exp", getItemData(itemId), 0)
        expCache[itemId] = exp
        return exp
    }

    fun getMaxLevelById(itemId: Int): Int {
        levelCache[itemId]?.let { return it }
        val level = DataTool.getIntConvert("info/maxLevel", getItemData(itemId), 256)
        levelCache[itemId] = level
        return level
    }

    fun getItemReward(itemId: Int): Pair<Int, List<RewardItem>> {
        rewardCache[itemId]?.let { return it }
        var totalProb = 0
        val rewards = mutableListOf<RewardItem>()
        getItemData(itemId)?.getChildByPath("reward")?.children?.forEach { child ->
            val reward = RewardItem()
            with (reward) {
                this.itemId = DataTool.getInt("item", child, 0)
                prob = DataTool.getInt("prob", child, 0).toShort()
                quantity = DataTool.getInt("count", child, 0).toShort()
                effect = DataTool.getString("Effect", child, "")
                worldMessage = DataTool.getStringNullable("worldMsg", child, null)
                period = DataTool.getInt("period", child, -1)
                totalProb += prob
            }
        }
        val hmm = Pair(totalProb, rewards.toList())
        rewardCache[itemId] = hmm
        return hmm
    }

    fun canWearEquipment(chr: Character, items: Collection<Item>): Collection<Item> {
        val inv = chr.getInventory(InventoryType.EQUIPPED) ?: return emptyList()
        if (inv.checked) return items
        val itemList = mutableListOf<Item>()
        if (chr.job == GameJob.SUPERGM || chr.job == GameJob.GM) {
            items.forEach { it as Equip
                it.isWearing = true
                itemList.add(it)
            }
            return itemList
        }
        val highFiveStamp = false
        /* Removed because players shouldn't even get this, and gm's should just be gm job.
         try {
         for (Pair<Item, InventoryType> ii : ItemFactory.INVENTORY.loadItems(chr.getId(), false)) {
         if (ii.getRight() == InventoryType.CASH) {
         if (ii.getLeft().getItemId() == 5590000) {
         highfivestamp = true;
         }
         }
         }
         } catch (SQLException ex) {
         }*/
        var tdex = chr.dex
        var tstr = chr.str
        var tint = chr.int
        var tluk = chr.luk
        val fame = chr.fame
        if (chr.job != GameJob.SUPERGM || chr.job != GameJob.GM) {
            inv.list().forEach { it as Equip
                tdex += it.dex
                tstr += it.str
                tint += it.int
                tluk += it.luk
            }
        }
        items.forEach { it as Equip
            var reqLevel = getEquipStats(it.itemId)?.get("reqLevel") ?: 0
            if (highFiveStamp) {
                reqLevel -= 5
                if (reqLevel < 0) reqLevel = 0
            }
            val stats = getEquipStats(it.itemId)
            if (reqLevel > chr.level) return@forEach
            if ((stats?.get("reqDEX") ?: 0) > tdex) return@forEach
            if ((stats?.get("reqSTR") ?: 0) > tstr) return@forEach
            if ((stats?.get("reqLUK") ?: 0) > tluk) return@forEach
            if ((stats?.get("reqINT") ?: 0) > tint) return@forEach
            val reqPop = stats?.get("reqPOP") ?: 0
            if (reqPop > 0) {
                if (reqPop > fame) return@forEach
            }
            it.isWearing = true
            itemList.add(it)
        }
        inv.checked = true
        return itemList
    }

    fun canWearEquipment(chr: Character, equip: Equip): Boolean {
        val list = canWearEquipment(chr, listOf(equip))
        return if (list.isNotEmpty()) {
            list.first() == equip
        } else {
            false
        }
    }

    fun getItemLevelUpStats(itemId: Int, level: Int, timeless: Boolean): List<Pair<String, Int>> {
        val list = mutableListOf<Pair<String, Int>>()
        val data = getItemData(itemId)?.getChildByPath("info")?.getChildByPath("level") ?: return emptyList()
        val data2 = data.getChildByPath("info")?.getChildByPath(level.toString()) ?: return emptyList()
        data2.children.forEach { mData ->
            if (Math.random() < 0.9) {
                listOf("DEX", "STR", "INT", "LUK", "MHP", "MMP", "PAD", "MAD", "PDD", "MDD", "ACC", "EVA", "Speed", "Jump").forEach { stat ->
                    if (data.name.startsWith("inc${stat}Min")) {
                        list.add(Pair("inc$stat", Character.rand(DataTool.getInt(mData), DataTool.getInt(data2.getChildByPath("inc${stat}Max")))))
                    }
                }
            }
        }
        return list

    }

    data class ScriptedItem(val npc: Int, val script: String, val runOnPickup: Boolean)

    class RewardItem {
        var itemId = 0
        var period = 0
        var prob: Short = 0
        var quantity: Short = 0
        var effect: String? = null
        var worldMessage: String? = null
    }
}