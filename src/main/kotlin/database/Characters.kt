/**
 * This package contains the database related classes and objects.
 */
package database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * This object represents the Characters table in the database.
 * It extends the Table class from the Exposed SQL library.
 */
object Characters : Table() {
    /**
     * This is the primary key column of the table.
     * It is an auto-incrementing integer and has a unique index.
     */
    val id = integer("id").autoIncrement().uniqueIndex()

    /**
     * This column represents the account ID associated with the character.
     * It is an integer and references the id column in the Accounts table.
     */
    val accountId = integer("accountId").references(Accounts.id)

    /**
     * This column represents the world of the character.
     * It is an integer with a default value of 0.
     */
    val world = integer("world").default(0)

    /**
     * This column represents the name of the character.
     * It is a varchar column with a maximum length of 13 characters and a default value of an empty string.
     */
    val name = varchar("name", 13).default("")

    /**
     * This column represents the level of the character.
     * It is an integer with a default value of 1.
     */
    val level = integer("level").default(1)

    /**
     * This column represents the experience points of the character.
     * It is an integer with a default value of 0.
     */
    val exp = integer("exp").default(0)

    /**
     * This column represents the gacha experience points of the character.
     * It is an integer with a default value of 0.
     */
    val gachaExp = integer("gachaExp").default(0)

    /**
     * This column represents the strength of the character.
     * It is an integer with a default value of 12.
     */
    val str = integer("str").default(12)

    /**
     * This column represents the dexterity of the character.
     * It is an integer with a default value of 5.
     */
    val dex = integer("dex").default(5)

    /**
     * This column represents the intelligence of the character.
     * It is an integer with a default value of 4.
     */
    val int = integer("int").default(4)

    /**
     * This column represents the luck of the character.
     * It is an integer with a default value of 4.
     */
    val luk = integer("luk").default(4)

    /**
     * This column represents the health points of the character.
     * It is an integer with a default value of 50.
     */
    val hp = integer("hp").default(50)

    /**
     * This column represents the magic points of the character.
     * It is an integer with a default value of 5.
     */
    val mp = integer("mp").default(5)

    /**
     * This column represents the maximum health points of the character.
     * It is an integer with a default value of 50.
     */
    val maxHp = integer("maxHp").default(50)

    /**
     * This column represents the maximum magic points of the character.
     * It is an integer with a default value of 5.
     */
    val maxMp = integer("maxMp").default(5)

    /**
     * This column represents the mesos (currency) of the character.
     * It is an integer with a default value of 0.
     */
    val meso = integer("meso").default(0)

    /**
     * This column represents the health and magic points used by the character.
     * It is an integer with a default value of 0.
     */
    val hpMpUsed = integer("hpMpUsed").default(0)

    /**
     * This column represents the job of the character.
     * It is an integer with a default value of 0.
     */
    val job = integer("job").default(0)

    /**
     * This column represents the skin color of the character.
     * It is an integer with a default value of 0.
     */
    val skinColor = integer("skinColor").default(0)

    /**
     * This column represents the gender of the character.
     * It is an integer with a default value of 0.
     */
    val gender = integer("gender").default(0)

    /**
     * This column represents the fame of the character.
     * It is an integer with a default value of 0.
     */
    val fame = integer("fame").default(0)

    /**
     * This column represents the hair of the character.
     * It is an integer with a default value of 0.
     */
    val hair = integer("hair").default(0)

    /**
     * This column represents the face of the character.
     * It is an integer with a default value of 0.
     */
    val face = integer("face").default(0)

    /**
     * This column represents the ability points of the character.
     * It is an integer with a default value of 0.
     */
    val ap = integer("ap").default(0)

    /**
     * This column represents the skill points of the character.
     * It is an integer with a default value of 0.
     */
    val sp = integer("sp").default(0)

    /**
     * This column represents the map of the character.
     * It is an integer with a default value of 0.
     */
    val map = integer("map").default(0)

    /**
     * This column represents the spawn point of the character.
     * It is an integer with a default value of 0.
     */
    val spawnPoint = integer("spawnPoint").default(0)

    /**
     * This column represents the gm status of the character.
     * It is a byte with a default value of 0.
     */
    val gm = byte("gm").default(0)

    /**
     * This column represents the party of the character.
     * It is an integer with a default value of 0.
     */
    val party = integer("party").default(0)

    /**
     * This column represents the buddy capacity of the character.
     * It is an integer with a default value of 25.
     */
    val buddyCapacity = integer("buddyCapacity").default(25)

    /**
     * This column represents the creation date of the character.
     * It is a timestamp with a default value of the current time.
     */
    val createDate = timestamp("createDate").clientDefault { Instant.now() }

    /**
     * This column represents the rank of the character.
     * It is an integer with a default value of 1.
     */
    val rank = integer("rank").default(1)

    /**
     * This column represents the rank move of the character.
     * It is an integer with a default value of 0.
     */
    val rankMove = integer("rankMove").default(0)

    /**
     * This column represents the job rank of the character.
     * It is an integer with a default value of 1.
     */
    val jobRank = integer("jobRank").default(1)

    /**
     * This column represents the job rank move of the character.
     * It is an integer with a default value of 0.
     */
    val jobRankMove = integer("jobRsetupslotsankMove").default(0)

    /**
     * This column represents the guild ID of the character.
     * It is an integer with a default value of 0.
     */
    val guildId = integer("guildId").default(0)

    /**
     * This column represents the guild rank of the character.
     * It is an integer with a default value of 5.
     */
    val guildRank = integer("guildRank").default(5)

    /**
     * This column represents the messenger ID of the character.
     * It is an integer with a default value of 0.
     */
    val messengerId = integer("messengerId").default(0)

    /**
     * This column represents the messenger position of the character.
     * It is an integer with a default value of 4.
     */
    val messengerPosition = integer("messengerPosition").default(4)

    /**
     * This column represents the mount level of the character.
     * It is an integer with a default value of 1.
     */
    val mountLevel = integer("mountLevel").default(1)

    /**
     * This column represents the mount experience points of the character.
     * It is an integer with a default value of 0.
     */
    val mountExp = integer("mountExp").default(0)

    /**
     * This column represents the mount tiredness of the character.
     * It is an integer with a default value of 0.
     */
    val mountTiredness = integer("mountTiredness").default(0)

    /**
     * This column represents the omok wins of the character.
     * It is an integer with a default value of 0.
     */
    val omokWins = integer("omokWins").default(0)

    /**
     * This column represents the omok losses of the character.
     * It is an integer with a default value of 0.
     */
    val omokLossess = integer("omokLosses").default(0)

    /**
     * This column represents the omok ties of the character.
     * It is an integer with a default value of 0.
     */
    val omokTies = integer("omokTies").default(0)

    /**
     * This column represents the match card wins of the character.
     * It is an integer with a default value of 0.
     */
    val matchCardWins = integer("matchCardWins").default(0)

    /**
     * This column represents the match card losses of the character.
     * It is an integer with a default value of 0.
     */
    val matchCardLossess = integer("matchCardLossess").default(0)

    /**
     * This column represents the match card ties of the character.
     * It is an integer with a default value of 0.
     */
    val matchCardTies = integer("matchCardTies").default(0)

    /**
     * This column represents the merchant mesos of the character.
     * It is an integer with a default value of 0.
     */
    val merchantMesos = integer("merchantMesos").default(0)

    /**
     * This column represents the merchant status of the character.
     * It is a boolean with a default value of false.
     */
    val hasMerchant = bool("hasMerchant").default(false)

    /**
     * This column represents the equip slots of the character.
     * It is an integer with a default value of 24.
     */
    val equipSlots = integer("equipSlots").default(24)

    /**
     * This column represents the use slots of the character.
     * It is an integer with a default value of 24.
     */
    val useSlots = integer("useSlots").default(24)

    /**
     * This column represents the setup slots of the character.
     * It is an integer with a default value of 24.
     */
    val setupSlots = integer("setupSlots").default(24)

    /**
     * This column represents the etc slots of the character.
     * It is an integer with a default value of 24.
     */
    val etcSlots = integer("etcSlots").default(24)

    /**
     * This column represents the monster book cover of the character.
     * It is an integer with a default value of 0.
     */
    val monsterBookCover = integer("monsterBookCover").default(0)

    /**
     * This column represents the summon value of the character.
     * It is an integer with a default value of 0.
     */
    val summonValue = integer("summonValue").default(0)

    /**
     * This column represents the pet health points of the character.
     * It is an integer with a default value of 0.
     */
    val petHp = integer("petHp").default(0)

    /**
     * This column represents the pet magic points of the character.
     * It is an integer with a default value of 0.
     */
    val petMp = integer("petMp").default(0)

    /**
     * This is the primary key of the table.
     * It is set to the id column.
     */
    override val primaryKey = PrimaryKey(id)
}