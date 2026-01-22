package client.command

import client.CharacterStat
import client.Client
import client.GameJob
import client.SkillFactory
import net.server.Server
import server.InventoryManipulator.Companion.addById
import server.ShopFactory
import server.life.LifeFactory
import server.life.Monster
import server.maps.MapObjectType
import tools.PacketCreator
import tools.packet.InteractPacket

/**
 * Commands that can be executed by GMs or for testing purposes
 */
object Commands {
    /**
     * Executes a GM command
     * @param c The client object that executing the command
     * @param sub The command and arguments split by spaces
     * @param heading The heading character that was used to execute the command
     */
    fun executeGMCommand(c: Client, sub: Array<String>, heading: Char): Boolean {
        when (val command = sub[0]) {
            "map" -> {
                val mapId = sub[1].toInt()
                if (!c.getChannelServer().mapFactory.isMapExists(mapId)) {
                    c.announce(InteractPacket.serverNotice(1, "Cannot find map with $mapId. You need to try again with different map id."))
                } else {
                    //val map = c.getChannelServer().mapFactory.getMap(mapId)
                    c.player?.changeMap(mapId)
                }
            }
            "gmshop" -> {
                ShopFactory.getShop(1337)?.sendShop(c)
            }
            "job" -> {
                GameJob.getById(sub[1].toInt())?.let {
                    c.player?.changeJob(it)
                    c.player?.equipChanged()
                    c.player?.setRates()
                } ?: c.announce(InteractPacket.serverNotice(1, "Job with id ${sub[1]} cannot be found."))
            }
            "cleardrops" -> c.player?.let { c.player?.map?.clearDrops(it) }
            "heal" -> c.player?.setHpMp(30000)
            "droprate" -> {
                c.getWorldServer().dropRate = sub[1].toInt()
                c.player?.dropRate = sub[1].toInt()
            }
            "levelup" -> c.player?.levelUp(false)
            "level2" -> {
                val times = sub[1].toInt()
                for (i in 0..times) {
                    c.player?.levelUp(false)
                }
            }
            "level" -> {
                c.player?.let { player ->
                    player.level = sub[1].toInt().toShort()
                    player.gainExp(-player.exp.get(), show = false, inChat = false, white = true)
                    player.updateSingleStat(CharacterStat.LEVEL, player.level.toInt(), false)
                }
            }
            "skillmaster" -> {
                val job = sub[1].toIntOrNull() ?: 0
                val skillData= SkillFactory.getSkillDataByJobId(job)
                skillData?.forEach loop@ {
                    if (it.name == "skill") {
                        it.forEach { s ->
                            val skill = SkillFactory.getSkill(s.name.toInt()) ?: return@forEach
                            c.player?.changeSkillLevel(skill, skill.getMaxLevel().toByte(), skill.getMaxLevel(), -1)
                        }
                    }
                }
                    c.player?.skills?.forEach { (t, _) ->

                        c.player?.changeSkillLevel(t, t.getMaxLevel().toByte(), t.getMaxLevel(), -1)
                    }
            }
            "stat" -> {
                val value = sub[2].toIntOrNull() ?: 0
                when (sub[1].lowercase()) {
                    "str" -> {
                        c.player?.str = value
                        c.player?.updateSingleStat(CharacterStat.STR, value)
                    }
                    "dex" -> {
                        c.player?.dex = value
                        c.player?.updateSingleStat(CharacterStat.DEX, value)
                    }
                }
            }
            "meso" -> {
                c.player?.gainMeso(sub[1].toIntOrNull() ?: 0, true)
            }
            "error" -> {
                c.player?.announce(PacketCreator.enableActions())
                c.player?.dropMessage(1, "Error fixed.")
            }
            "item" -> {
                val itemId = sub[1].toIntOrNull() ?: 0
                val quantity = sub[2].toIntOrNull() ?: 0
                addById(c, itemId, quantity.toShort())
            }
            "spawn" -> {
                val mobId = sub[1].toIntOrNull() ?: -1
                val monster = LifeFactory.getMonster(mobId)
                if (monster == null) {
                    c.player?.dropMessage(1, "Monster with id $mobId cannot be found.")
                    return false
                }
                val pos = c.player?.position ?: return false
                if (sub.size > 2) {
                    for (i in 0 until sub[2].toInt()) {
                        c.player?.map?.spawnMonsterOnGroundBelow(monster, pos)
                    }
                } else {
                    c.player?.map?.spawnMonsterOnGroundBelow(monster, pos)
                }
            }
            "killall" -> {
                c.player?.let { player ->
                    val monsters = player.map.getMapObjectsInRange(
                        player.position,
                        Double.POSITIVE_INFINITY,
                        listOf(MapObjectType.MONSTER)
                    )
                    val map = player.map
                    for (mob in monsters) {
                        mob as Monster
                        map.killMonster(mob, player, withDrops = true, secondTime = false, animation = 1)
                        mob.giveExpToCharacter(player, mob.stats.exp * player.expRate, true, 1)
                    }
                    player.dropMessage(0, "Killed ${monsters.size} monsters.")
                }
            }
            "servermessage" -> {
                val message = sub.getOrNull(1) ?: ""
                c.player?.let { player ->
                    val currentWorldId = player.world
                    Server.getWorld(currentWorldId).setServerMessage(message)
                }
            }
            else -> c.announce(InteractPacket.serverNotice(1, "Unknown command line: $command"))
        }
        return true
    }
}
    /*fun executePlayerCommand(c: Client, sub: Array<String>, heading: Char): Boolean {
        val chr = c.player
        if (heading != '@') {
            return false
        }
        when (sub[0]) {
            "삭제" -> {
                println("" + RecvPacketOpcode.TROCK_ADD_MAP.value)
                start(c, 9900000, "cl", chr)
            }
            "rape" -> {
                val list: MutableList<Pair<BuffStat, Int>> = ArrayList()
                list.add(Pair(BuffStat.MORPH, 8))
                list.add(Pair(BuffStat.CONFUSE, 1))
                chr!!.announce(PacketCreator.giveBuff(0, 0, list))
                chr.map.broadcastMessage(chr, PacketCreator.giveForeignBuff(chr.id, list))
            }
            "렉" -> {
                dispose(c)
                c.session!!.write(PacketCreator.enableActions())
            }
            else -> {
                if (chr!!.gmLevel == 0) {
                    chr.yellowMessage("Player Command " + heading + sub[0] + " does not exist")
                }
                return false
            }
        }
        return true
    }

    fun executeGMCommand(c: Client, sub: Array<String>, heading: Char): Boolean {
        val player = c.player
        val cserv = c.getChannelServer()
        val srv = Server
        val any = if (sub[0] == "스탯포인트") {
            player!!.remainingAp = sub[1].toInt()
        } else if (sub[0] == "버프") {
            val array = intArrayOf(2001002, 1101007, 2301003, 1111002, 4111001, 4111002, 4211003, 4211005, 1321000)
            for (i in array) {
                getSkill(i)!!
                    .getEffect(getSkill(i)!!.getMaxLevel()).applyTo(player!!)
            }
        } else if (sub[0] == "스폰") {

        } else if (sub[0] == "이동") {
            var victim = cserv.players.getCharacterByName(sub[1])
            if (victim != null) {
                if (sub.size == 2) {
                    val target = victim.map
                    c.player!!.changeMap(target, target.findClosestSpawnPoint(victim.position))
                } else {
                    val mapid = sub[2].toInt()
                    val target = c.getWorldServer().getChannel(victim.client.channel).mapFactory.getMap(mapid)
                    victim.changeMap(target, target.getPortal(0))
                }
            } else {
                try {
                    victim = c.player
                    val map = sub[1].toInt()
                    val target = cserv.mapFactory.getMap(map)
                    c.player!!.changeMap(target, target.getPortal(0))
                } catch (e: Exception) {
                    c.player!!.dropMessage(0, "Something went wrong " + e.message)
                }
            }
        } else if (sub[0] == "소환") {
            val victim = cserv.players.getCharacterByName(sub[1])
            if (victim != null) {
                if (sub.size == 2) {
                    val target = c.player!!.map
                    victim.changeMap(target, target.findClosestSpawnPoint(c.player!!.position))
                }
            }
        } else if (sub[0] == "모두소환") {
            val target = c.player!!.map //공통
            for (victim in c.getWorldServer().players.getAllCharacters()) {
                if (victim != null && c.player!!.id != victim.id) {
                    victim.changeMap(target, target.findClosestSpawnPoint(c.player!!.position))
                }
            }
        } else if (sub[0] == "청소") {
            player!!.map.clearDrops(player)
        } else if (sub[0] == "지엠채팅") {
            val message = joinStringFrom(sub, 1)
            gmChat(player!!.name + " : " + message, "")
        } else if (sub[0] == "팅") {
            var victim = c.getWorldServer().players.getCharacterByName(sub[1])
            if (victim == null) {
                victim = c.getChannelServer().players.getCharacterByName(sub[1])
                if (victim == null) {
                    victim = player!!.map.getCharacterByName(sub[1])
                    if (victim != null) {
                        try { //sometimes bugged because the map = null
                            victim.client.session!!.close()
                            player.map.removePlayer(victim)
                        } catch (e: Exception) {
                        }
                    } else {
                        return true
                    }
                }
            }
            if (player!!.gmLevel < victim.gmLevel) {
                victim = player
            }
            victim.client.disconnect(false, false)
            /*} else if (sub[0].equals("경험치배율")) {
            c.getWorldServer().setExpRate(Integer.parseInt(sub[1]));
            for (Character mc : c.getWorldServer().getPlayers().getAllCharacters()) {
                mc.setRates();
            }*/
        } else if (sub[0] == "옵코드리셋") {
            SendPacketOpcode.reloadOpcode()
            RecvPacketOpcode.reloadOpcode()
        } else if (sub[0] == "인기도") {
            val victim = cserv.players.getCharacterByName(sub[1])
            victim!!.fame = sub[2].toInt()
            victim.updateSingleStat(CharacterStat.FAME, victim.fame, false)
        } else if (sub[0] == "캐시지급") {
            cserv.players.getCharacterByName(sub[1])!!.cashShop!!.gainCash(1, sub[2].toInt())
            player!!.message("지급이 완료되었습니다.")
        } else if (sub[0] == "지엠상점") {
            getShop(1337)!!.sendShop(c)
        } else if (sub[0] == "힐") {
            player!!.setHpMp(30000)
        } else if (sub[0] == "id") {
            try {
                BufferedReader(
                    InputStreamReader(
                        URL("http://www.maptip.com/search_java.php?search_value=" + sub[1] + "&check=true").openConnection()
                            .getInputStream()
                    )
                ).use { dis ->
                    var s: String?
                    while (dis.readLine().also { s = it } != null) {
                        player!!.dropMessage(0, s!!)
                    }
                }
            } catch (e: Exception) {
            }
        } else if (sub[0] == "아이템" || sub[0] == "드롭") {
            val itemId = sub[1].toInt()
            var quantity: Short = 1
            try {
                quantity = sub[2].toShort()
            } catch (e: Exception) {
            }
            if (sub[0] == "아이템") {
                var petid = -1
                if (isPet(itemId)) {
                    petid = createPet(itemId)
                }
                addById(c, itemId, quantity, player!!.name, petid, -1)
            } else {
                val toDrop: Item
                toDrop = if (getInventoryType(itemId) === InventoryType.EQUIP) {
                    getEquipById(itemId)
                } else {
                    Item(itemId, 0.toByte(), quantity, -1)
                }
                c.player!!.map.spawnItemDrop(c.player!!, c.player, toDrop, c.player!!.position, true, true)
            }
        } else if (sub[0] == "직업") {
            player!!.changeJob(getById(sub[1].toInt())!!)
            player.equipChanged()
            player.setRates()
        } else if (sub[0] == "킬") {
            if (sub.size >= 2) {
                cserv.players.getCharacterByName(sub[1])!!.setHpMp(0)
            }
        } else if (sub[0] == "킬올") {
            val monsters = player!!.map.getMapObjectsInRange(
                player.position,
                Double.POSITIVE_INFINITY,
                Arrays.asList(MapObjectType.MONSTER)
            )
            val map = player.map
            for (monstermo in monsters) {
                val monster = monstermo as Monster
                map.killMonster(monster, player, true, false, 1)
                monster.giveExpToCharacter(player, monster.stats.exp * c.player!!.expRate, true, 1)
            }
            player.dropMessage(0, "Killed " + monsters.size + " monsters.")
        } else if (sub[0] == "monsterdebug") {
            val monsters = player!!.map.getMapObjectsInRange(
                player.position,
                Double.POSITIVE_INFINITY,
                Arrays.asList(MapObjectType.MONSTER)
            )
            for (monstermo in monsters) {
                val monster = monstermo as Monster
                player.message("Monster ID: " + monster.id)
            }
        } else if (sub[0] == "unbug") {
            c.player!!.map.broadcastMessage(PacketCreator.enableActions())
        } else if (sub[0] == "레벨") {
            player!!.level = sub[1].toInt().toShort()
            player.gainExp(-player.exp.get(), false, false, true)
            player.updateSingleStat(CharacterStat.LEVEL, player.level.toInt(), false)
        } else if (sub[0] == "levelperson") {
            val victim = cserv.players.getCharacterByName(sub[1])
            victim!!.level = sub[2].toInt().toShort()
            victim.gainExp(-victim.exp.get(), false, false, true)
            victim.updateSingleStat(CharacterStat.LEVEL, victim.level.toInt(), false)
        } else if (sub[0] == "levelpro") {
            while (player!!.level < Math.min(255, sub[1].toInt())) {
                player.levelUp(false)
            }
        } else if (sub[0] == "레벨업") {
            player!!.levelUp(false)
        } else if (sub[0] == "맥스스탯") {
            val s = arrayOf("setall", Short.MAX_VALUE.toString())
            executeGMCommand(c, s, heading)
            player!!.level = 255.toShort()
            player.fame = 13337
            player.maxHp = 30000
            player.maxMp = 30000
            player.updateSingleStat(CharacterStat.LEVEL, 255, false)
            player.updateSingleStat(CharacterStat.FAME, 13337, false)
            player.updateSingleStat(CharacterStat.MAXHP, 30000, false)
            player.updateSingleStat(CharacterStat.MAXMP, 30000, false)
        } else if (sub[0] == "맥스스킬") {
            getDataProvider(File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img")?.children?.forEach { skill_ ->
                try {
                    val skill = getSkill(skill_.name.toInt())
                    player!!.changeSkillLevel(skill!!, skill.getMaxLevel().toByte(), skill.getMaxLevel(), -1)
                } catch (nfe: NumberFormatException) {
                    return@forEach
                } catch (npe: NullPointerException) {
                    return@forEach
                }
            }
        } else if (sub[0] == "스킬초기화") {
            for (skill_ in getDataProvider(File(System.getProperty("wzpath") + "/" + "String.wz")).getData("Skill.img")?.children) {
                try {
                    val skill = getSkill(skill_.name.toInt())
                    player!!.changeSkillLevel(skill!!, 0.toByte(), skill.getMaxLevel(), -1)
                } catch (nfe: NumberFormatException) {
                    break
                } catch (npe: NullPointerException) {
                    continue
                }
            }
        } else if (sub[0] == "mesoperson") {
            //cserv.getPlayers().getCharacterByName(sub[1]).gainMeso(Integer.parseInt(sub[2]), true);
        } else if (sub[0] == "메소") {
//            player.gainMeso(Integer.parseInt(sub[1]), true);
        } else if (sub[0] == "공지") {
            broadcastMessage(player!!.world, MessagePacket.serverNotice(6, "[공지사항] " + joinStringFrom(sub, 1)))
        } else if (sub[0] == "openportal") {
            player!!.map.getPortal(sub[1])!!.portalState = true
        } else if (sub[0] == "closeportal") {
            player!!.map.getPortal(sub[1])!!.portalState = false
        } else if (sub[0] == "startevent") {
            for (chr in player!!.map.characters) {
                player.map.startEvent(chr)
            }
            c.getChannelServer().event = null
        } else if (sub[0] == "scheduleevent") {
            if (c.player!!.map.hasEventNpc()) {
                when (sub[1]) {
                    "treasure" -> c.getChannelServer().event = Event(109010000, 50)
                    "ox" -> {
                        c.getChannelServer().event = Event(109020001, 50)
                        srv.broadcastMessage(
                            player!!.world,
                            MessagePacket.serverNotice(
                                0,
                                "Hello Scania let's play an event in " + player.map.mapName + " CH " + c.channel + "! " + player.map.getEventNpc()
                            )
                        )
                    }
                    "ola" -> {
                        c.getChannelServer().event = Event(109030101, 50) // Wrong map but still Ola Ola
                        srv.broadcastMessage(
                            player!!.world,
                            MessagePacket.serverNotice(
                                0,
                                "Hello Scania let's play an event in " + player.map.mapName + " CH " + c.channel + "! " + player.map.getEventNpc()
                            )
                        )
                    }
                    "fitness" -> {
                        c.getChannelServer().event = Event(109040000, 50)
                        srv.broadcastMessage(
                            player!!.world,
                            PacketCreator.serverNotice(
                                0,
                                "Hello Scania let's play an event in " + player.map.mapName + " CH " + c.channel + "! " + player.map.getEventNpc()
                            )
                        )
                    }
                    "snowball" -> {
                        c.getChannelServer().event = Event(109060001, 50)
                        srv.broadcastMessage(
                            player!!.world,
                            PacketCreator.serverNotice(
                                0,
                                "Hello Scania let's play an event in " + player.map.mapName + " CH " + c.channel + "! " + player.map.getEventNpc()
                            )
                        )
                    }
                    "coconut" -> {
                        c.getChannelServer().event = Event(109080000, 50)
                        srv.broadcastMessage(
                            player!!.world,
                            PacketCreator.serverNotice(
                                0,
                                "Hello Scania let's play an event in " + player.map.mapName + " CH " + c.channel + "! " + player.map.getEventNpc()
                            )
                        )
                    }
                    else -> player!!.message("Wrong Syntax: /scheduleevent treasure, ox, ola, fitness, snowball or coconut")
                }
            } else {
                player!!.message("You can only use this command in the following maps: 60000, 104000000, 200000000, 220000000")
            }
        } else if (sub[0] == "온라인") {
            for (ch in srv.getChannelsFromWorld(player!!.world)) {
                var s = "(채널 " + ch.channelId + " / 접속자 : " + ch.players.getAllCharacters().size + "명) : "
                if (ch.players.getAllCharacters().size < 50) {
                    for (chr in ch.players.getAllCharacters()) {
                        s += makeReadable(chr.name) + ", "
                    }
                    player.dropMessage(0, s.substring(0, s.length - 2))
                }
            }
        } else if (sub[0] == "파풀라투스") {
            player!!.map.spawnMonsterOnGroundBelow(getMonster(8500001)!!, player.position)
        } else if (sub[0] == "피아누스") {
            player!!.map.spawnMonsterOnGroundBelow(getMonster(8510000)!!, player.position)
        } else if (sub[0] == "검색") {
            if (sub.size == 1) {
                c.player!!.dropMessage(6, sub[0] + ": <엔피시> <몹> <아이템> <맵> <스킬>")
            } else {
                //   splitted[2] = KorConvertHandler.KorConvert(splitted[2]);
                val type = sub[1]
                val search = StringUtil.joinStringFrom(sub, 2)
                var data: Data? = null
                val dataProvider = getDataProvider(File("wz/String.wz"))
                c.player!!.dropMessage(6, "<<타입: $type | 검색어: $search>>")
                if (type.equals("엔피시", ignoreCase = true)) {
                    val retNpcs: MutableList<String> = ArrayList()
                    data = dataProvider.getData("Npc.img")
                    val npcPairList: MutableList<Pair<Int, String>> = LinkedList()
                    if (data != null) {
                        for (npcIdData in data.children) {
                            npcPairList.add(
                                Pair(
                                    npcIdData.name.toInt(),
                                    DataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")
                                )
                            )
                        }
                    }
                    for ((first, second) in npcPairList) {
                        if (second.lowercase(Locale.getDefault()).contains(search.lowercase(Locale.getDefault()))) {
                            retNpcs.add("$first - $second")
                        }
                    }
                    if (retNpcs != null && retNpcs.size > 0) {
                        for (singleRetNpc in retNpcs) {
                            c.player!!.dropMessage(6, singleRetNpc)
                        }
                    } else {
                        c.player!!.dropMessage(6, "발견된 엔피시가 없습니다.")
                    }
                } else if (type.equals("맵", ignoreCase = true)) {
                    val retMaps: MutableList<String> = ArrayList()
                    data = dataProvider.getData("Map.img") ?: return false
                    val mapPairList: MutableList<Pair<Int, String>> = LinkedList()
                    for (mapAreaData in data.children) {
                        for (mapIdData in mapAreaData.children) {
                            mapPairList.add(
                                Pair(
                                    mapIdData.name.toInt(),
                                    DataTool.getString(
                                        mapIdData.getChildByPath("streetName"),
                                        "NO-NAME"
                                    ) + " -  " + DataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")
                                )
                            )
                        }
                    }
                    for ((first, second) in mapPairList) {
                        if (second.lowercase(Locale.getDefault()).contains(search.lowercase(Locale.getDefault()))) {
                            retMaps.add("$first - $second")
                        }
                    }
                    if (retMaps != null && retMaps.size > 0) {
                        for (singleRetMap in retMaps) {
                            c.player!!.dropMessage(6, singleRetMap)
                        }
                    } else {
                        c.player!!.dropMessage(6, "발견된 맵이 없습니다.")
                    }
                } else if (type.equals("몹", ignoreCase = true)) {
                    val retMobs: MutableList<String> = ArrayList()
                    data = dataProvider.getData("Mob.img")
                    val mobPairList: MutableList<Pair<Int, String>> = LinkedList()
                    if (data != null) {
                        for (mobIdData in data.children) {
                            mobPairList.add(
                                Pair(
                                    mobIdData.name.toInt(),
                                    DataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")
                                )
                            )
                        }
                    }
                    for ((first, second) in mobPairList) {
                        if (second.lowercase(Locale.getDefault()).contains(search.lowercase(Locale.getDefault()))) {
                            retMobs.add("$first - $second")
                        }
                    }
                    if (retMobs != null && retMobs.size > 0) {
                        for (singleRetMob in retMobs) {
                            c.player!!.dropMessage(6, singleRetMob)
                        }
                    } else {
                        c.player!!.dropMessage(6, "발견된 몬스터가 없습니다.")
                    }
                } else if (type.equals("REACTOR", ignoreCase = true)) {
                    c.player!!.dropMessage(6, "NOT ADDED YET")
                } else if (type.equals("아이템", ignoreCase = true)) {
                    val retItems: MutableList<String> = ArrayList()
                    for ((first, second) in getAllItems()) {
                        if (second.lowercase(Locale.getDefault()).contains(search.lowercase(Locale.getDefault()))) {
                            retItems.add("$first - $second")
                        }
                    }
                    if (retItems != null && retItems.size > 0) {
                        for (singleRetItem in retItems) {
                            c.player!!.dropMessage(6, singleRetItem)
                        }
                    } else {
                        c.player!!.dropMessage(6, "발견된 아이템이 없습니다.")
                    }
                } else if (type.equals("스킬", ignoreCase = true)) {
                    val retSkills: MutableList<String> = ArrayList()
                    data = dataProvider.getData("Skill.img")
                    val skillPairList: MutableList<Pair<Int, String>> = LinkedList()
                    for (skillIdData in data?.children!!) {
                        skillPairList.add(
                            Pair(
                                skillIdData.name.toInt(),
                                DataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME")
                            )
                        )
                    }
                    for ((first, second) in skillPairList) {
                        if (second.lowercase(Locale.getDefault()).contains(search.lowercase(Locale.getDefault()))) {
                            retSkills.add("$first - $second")
                        }
                    }
                    if (retSkills != null && retSkills.size > 0) {
                        for (singleRetSkill in retSkills) {
                            c.player!!.dropMessage(6, singleRetSkill)
                        }
                    } else {
                        c.player!!.dropMessage(6, "발견된 스킬이 없습니다.")
                    }
                } else {
                    c.player!!.dropMessage(6, "해당 검색은 처리할 수 없습니다.")
                }
            }
            /*        } else if (sub[0].equalsIgnoreCase("검색")) {
            StringBuilder sb = new StringBuilder();
            if (sub.length > 2) {
                String search = joinStringFrom(sub, 2);
                long start = System.currentTimeMillis();//for the lulz
                Data data = null;
                DataProvider dataProvider = DataProviderFactory.getDataProvider(new File("wz/String.wz"));
                if (!sub[1].equalsIgnoreCase("아이템")) {
                    if (sub[1].equalsIgnoreCase("엔피시")) {
                        data = dataProvider.getData("Npc.img");
                    } else if (sub[1].equalsIgnoreCase("몬스터") || sub[1].equalsIgnoreCase("MONSTER")) {
                        data = dataProvider.getData("Mob.img");
                    } else if (sub[1].equalsIgnoreCase("스킬")) {
                        data = dataProvider.getData("Skill.img");
                    } else if (sub[1].equalsIgnoreCase("맵")) {
                        data = dataProvider.getData("Map.img");
                    } else if (sub[1].equalsIgnoreCase("퀘스트")) {
                        data = dataProvider.getData("Quest.img");
                        sb.append("#bUse the '/m' command to find a map. If it finds a map with the same name, it will warp you to it.");
                    } else {
                        sb.append("#bInvalid search.\r\nSyntax: '/search [type] [name]', where [type] is NPC, ITEM, MOB, or SKILL.");
                    }
                    if (data != null) {
                        String name;
                        for (Data searchData : data.getChildren()) {
                            name = DataTool.getString(searchData.getChildByPath("name"), "NO-NAME");
                            if (name.toLowerCase().contains(search.toLowerCase())) {
                                sb.append("#b").append(Integer.parseInt(searchData.getName())).append("#k - #r").append(name).append("\r\n");
                            }
                        }
                    }
                } else {
                    for (Pair<Integer, String> itemPair : ItemInformationProvider.INSTANCE.getAllItems()) {
                        if (sb.length() < 32654) {//ohlol
                            if (itemPair.getSecond().toLowerCase().contains(search.toLowerCase())) {
                                //#v").append(id).append("# #k- 
                                sb.append("#b").append(itemPair.getFirst()).append("#k - #r").append(itemPair.getSecond()).append("\r\n");
                            }
                        } else {
                            sb.append("#bCouldn't load all items, there are too many results.\r\n");
                            break;
                        }
                    }
                }
                if (sb.length() == 0) {
                    sb.append("#bNo ").append(sub[1].toLowerCase()).append("s found.\r\n");
                }

                sb.append("\r\n#kLoaded within ").append((double) (System.currentTimeMillis() - start) / 1000).append(" seconds.");//because I can, and it's free

            } else {
                sb.append("#bInvalid search.\r\nSyntax: '/search [type] [name]', where [type] is NPC, ITEM, MOB, or SKILL.");
            }
            c.announce(PacketCreator.getNPCTalk(9010000, (byte) 0, sb.toString(), "00 00"));

        } else if (sub[0] == "서버공지") {
            c.getWorldServer().setServerMessage(joinStringFrom(sub, 1))
        } else if (sub[0] == "warpsnowball") {
            for (chr in player!!.map.characters) {
                chr.changeMap(109060000, chr.eventTeam)
            }
        } else if (sub[0] == "setall") {
            val x = sub[1].toShort().toInt()
            player!!.str = x
            player.dex = x
            player.int = x
            player.luk = x
            player.updateSingleStat(CharacterStat.STR, x, false)
            player.updateSingleStat(CharacterStat.DEX, x, false)
            player.updateSingleStat(CharacterStat.INT, x, false)
            player.updateSingleStat(CharacterStat.LUK, x, false)
        } else if (sub[0] == "스킬포인트") {
            player!!.remainingSp = sub[1].toInt()
            player.updateSingleStat(CharacterStat.AVAILABLESP, player.remainingSp, false)
        } else if (sub[0] == "unban") {
            try {
                /*try (PreparedStatement p = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = -1 WHERE id = " + Character.Companion.getIdByName(sub[1]))) {
                    p.executeUpdate();
                }*/
            } catch (e: Exception) {
                player!!.message("Failed to unban " + sub[1])
                return true
            }
            player!!.message("Unbanned " + sub[1])
        } else {
            return false
        }
        return true
    }

    fun executeAdminCommand(c: Client, sub: Array<String>, heading: Char) {
        val player = c.player
        when (sub[0]) {
            "horntail" -> player!!.map.spawnMonsterOnGroundBelow(getMonster(8810026)!!, player.position)
            "packet" -> player!!.map.broadcastMessage(PacketCreator.customPacket(joinStringFrom(sub, 1)))
            "warpworld" -> {
                val server = Server
                val worldb = sub[1].toByte()
                if (worldb <= server.worlds.size - 1) {
                    try {
                        val socket = server.getIp(worldb.toInt(), c.channel)!!.split(":").toTypedArray()
                        c.getWorldServer().removePlayer(player!!)
                        player.map.removePlayer(player) //LOL FORGOT THIS ><                    
                        c.updateLoginState(Client.LOGIN_SERVER_TRANSITION)
                        player.world = worldb.toInt()
                        player.saveToDatabase() //To set the new world :O (true because else 2 player instances are created, one in both worlds)
                        c.announce(
                            PacketCreator.getChannelChange(
                                InetAddress.getByName(socket[0]),
                                socket[1].toInt()
                            )
                        )
                    } catch (ex: UnknownHostException) {
                        player!!.message("Error when trying to change worlds, are you sure the world you are trying to warp to has the same amount of channels?")
                    } catch (ex: NumberFormatException) {
                        player!!.message("Error when trying to change worlds, are you sure the world you are trying to warp to has the same amount of channels?")
                    }
                } else {
                    player!!.message("Invalid world; highest number available: " + (server.worlds.size - 1))
                }
            }
            "모두저장" -> for (world in worlds) {
                for (chr in world.players.getAllCharacters()) {
                    chr.saveToDatabase()
                }
            }
            "모두드롭" -> {
                player!!.map.allDropItem(player)
                return
            }
            "고정엔피시" -> {
                if (sub.size < 1) {
                    c.player!!.dropMessage(6, "!고정엔피시 <id>")
                }
                val npcid = sub[1].toInt()
                val npcs = getNpc(npcid)
                if (npcs != null && npcs.stats.name != "MISSINGNO") {
                    val xpos = c.player!!.position.x
                    val ypos = c.player!!.position.y
                    val fh = c.player!!.map.footholds!!.findBelow(c.player!!.position)!!.id
                    npcs.position = c.player!!.position
                    npcs.cy = ypos
                    npcs.rx0 = xpos
                    npcs.rx1 = xpos
                    npcs.fh = fh
                    /*try {
                        Connection con = DatabaseConnection.getConnection();
                        try (PreparedStatement ps = con.prepareStatement("INSERT INTO wz_customlife (dataid, f, hide, fh, cy, rx0, rx1, type, x, y, mid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                            ps.setInt(1, npcid);
                            ps.setInt(2, 0); // 1 = right , 0 = left
                            ps.setInt(3, 0); // 1 = hide, 0 = show
                            ps.setInt(4, fh);
                            ps.setInt(5, ypos);
                            ps.setInt(6, xpos);
                            ps.setInt(7, xpos);
                            ps.setString(8, "n");
                            ps.setInt(9, xpos);
                            ps.setInt(10, ypos);
                            ps.setInt(11, c.getPlayer().getMapId());
                            ps.executeUpdate();
                        }
                    } catch (SQLException e) {
                        c.getPlayer().dropMessage(6, "엔피시 생성 오류");
                    }*/
                } else {
                    c.player!!.dropMessage(6, "생성불가능")
                }
                player!!.map.addMapObject(npcs)
                player.map.broadcastMessage(spawnNpc(npcs))
                c.player!!.dropMessage(6, "엔피시가 정상적으로 생성")
            }
            "엔피시" -> {
                if (sub.isEmpty()) {
                    return
                }
                val npc = getNpc(sub[1].toInt())
                npc.position = player!!.position
                npc.cy = player.position.y
                npc.rx0 = player.position.x + 50
                npc.rx1 = player.position.x - 50
                npc.fh = player.map.footholds!!.findBelow(c.player!!.position)!!.id
                player.map.addMapObject(npc)
                player.map.broadcastMessage(spawnNpc(npc))
            }
            "jobperson" -> {
                val victim = c.getChannelServer().players.getCharacterByName(sub[1])
                victim!!.changeJob(getById(sub[2].toInt())!!)
                player!!.equipChanged()
            }
            "pinkbean" -> player!!.map.spawnMonsterOnGroundBelow(getMonster(8820009)!!, player.position)
            "playernpc" -> player!!.playerNpc(c.getChannelServer().players.getCharacterByName(sub[1])!!, sub[2].toInt())
            "setgmlevel" -> {
                val victim = c.getChannelServer().players.getCharacterByName(sub[1])
                victim!!.gmLevel = sub[2].toInt()
                player!!.message("Done.")
                victim.client.disconnect(false, false)
            }
            "shutdown", "서버종료" -> {
                var time = 60000
                if (sub[0] == "서버종료") {
                    time = 1
                } else if (sub.size > 1) {
                    time *= sub[1].toInt()
                }
                schedule(shutdown(false), time.toLong())
            }
            "서버저장" -> {
                allSave(player!!.world)
                broadcastGMMessage(player.world, PacketCreator.serverNotice(5, "[알림] 명령어에 의해 월드가 저장되었습니다."))
            }
            "sql" -> {
                val query = joinStringFrom(sub, 1)
            }
            "sqlwithresult" -> {
                val name = sub[1]
                val query = joinStringFrom(sub, 2)
            }
            "itemvac" -> {
                val items = player!!.map.getMapObjectsInRange(
                    player.position,
                    Double.POSITIVE_INFINITY,
                    Arrays.asList(MapObjectType.ITEM)
                )
                for (item in items) {
                    val mapitem = item as MapItem
                    if (!addFromDrop(c, mapitem.item!!, true)) {
                        continue
                    }
                    mapitem.pickedUp = true
                    player.map.broadcastMessage(
                        PacketCreator.removeItemFromMap(mapitem.objectId, 2, player.id),
                        mapitem.position
                    )
                    player.map.removeMapObject(item)
                }
            }
            "zakum" -> {
                player!!.map.spawnFakeMonsterOnGroundBelow(getMonster(8800000)!!, player.position)
                var x = 8800003
                while (x < 8800011) {
                    player.map.spawnMonsterOnGroundBelow(getMonster(x)!!, player.position)
                    x++
                }
            }
            else -> player!!.yellowMessage("Command " + heading + sub[0] + " does not exist.")
        }
    }

    private fun ranking(rk: Boolean) {}

    /*try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (!rk) {
                ps = con.prepareStatement("SELECT level, name, job FROM characters WHERE gm < 1 ORDER BY level desc LIMIT 10");
            } else {
                ps = con.prepareStatement("SELECT name, gm FROM characters WHERE gm >= 1");
            }
            return ps.executeQuery();
        } catch (SQLException ex) {
            return null;
        }*/
    private fun joinStringFrom(arr: Array<String>, start: Int): String {
        val builder = StringBuilder()
        for (i in start until arr.size) {
            builder.append(arr[i])
            if (i != arr.size - 1) {
                builder.append(" ")
            }
        }
        return builder.toString()
    }
}*/