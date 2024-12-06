package client.autoban

import client.Character

/**
 * This class manages the autoban system for a character in the game.
 *
 * @property chr The character that this autoban manager is associated with.
 * @property points A mutable map that stores the autoban points for each autoban factory.
 * @property lastTime A mutable map that stores the last time a point was added for each autoban factory.
 * @property misses The number of misses that the character has made.
 * @property lastMisses The number of misses that the character made in the last check.
 * @property sameMissCount The number of times the character has made the same number of misses consecutively.
 * @property spam An array that stores the timestamps of the last spam for each type.
 * @property timestamp An array that stores the timestamps for each type.
 * @property timestampCounter An array that stores the number of times the same timestamp has been used for each type.
 */
class AutobanManager(val chr: Character) {
    val points = mutableMapOf<AutobanFactory, Int>()
    val lastTime = mutableMapOf<AutobanFactory, Long>()
    var misses = 0
    var lastMisses = 0
    var sameMissCount = 0
    var spam = LongArray(20)
    var timestamp = IntArray(20)
    var timestampCounter = ByteArray(20)

    /**
     * Adds a point to the autoban system for a specific autoban factory and reason.
     *
     * This function first checks if the last time a point was added for the given autoban factory is older than its expiration time.
     * If it is, the points for this factory are halved.
     *
     * Then, if the expiration time for the factory is not set to -1 (which would mean it never expires), the last time a point was added is updated to the current time.
     *
     * After that, a point is added to the factory's total points.
     *
     * Finally, if the total points for the factory have reached the limit set by the factory, and the character associated with this autoban manager is not a Game Master (GM),
     * an autoban is initiated for the character with a message that includes the name of the autoban factory and the provided reason.
     *
     * @param fac The autoban factory for which the point will be added.
     * @param reason The reason for adding the point.
     * @see AutobanFactory for the definition of an autoban factory.
     * @see Character for the definition of a character.
     * @see Character.autoban for the method that initiates an autoban for a character.
     */
    fun addPoint(fac: AutobanFactory, reason: String) {
        if (lastTime.containsKey(fac)) {
            if ((lastTime[fac] ?: 0) < (System.currentTimeMillis() - fac.expire)) {
                points[fac] = (points[fac] ?: 1) / 2
            }
        }
        if (fac.expire != -1L)
            lastTime[fac] = System.currentTimeMillis()
        points[fac] = (points[fac] ?: 0) + 1
        if ((points[fac] ?: 0) >= fac.points) {
            if (chr.isGM()) return
            chr.autoban("Autobanned for ${fac.name} ;$reason", 1)
        }
    }

    /**
     * Increases the number of misses by one.
     *
     * This function is used to increment the number of misses that the character associated with this autoban manager has made.
     * A miss is defined as an action that the autoban system has detected as potentially suspicious, but not enough to warrant an autoban.
     * The number of misses is used in the autoban decision process, with a higher number of misses increasing the likelihood of an autoban.
     *
     * @see AutobanManager for the definition of an autoban manager and its associated properties.
     */
    fun addMiss() = misses++

    /**
     * Resets the number of misses and checks if the character should be autobanned.
     *
     * This function first checks if the last number of misses is equal to the current number of misses and if the number of misses is greater than 6.
     * If both conditions are met, the count of consecutive times the same number of misses has been made is incremented.
     *
     * Then, if the count of consecutive times the same number of misses has been made is greater than 4, an autoban is initiated for the character with a message that includes the number of misses.
     * If the count of consecutive times the same number of misses has been made is not greater than 4 but is greater than 0, the last number of misses is updated to the current number of misses.
     *
     * Finally, the number of misses is reset to 0.
     *
     * @see AutobanManager for the definition of an autoban manager and its associated properties.
     * @see Character for the definition of a character.
     * @see Character.autoban for the method that initiates an autoban for a character.
     */
    fun resetMisses() {
        if (lastMisses == misses && misses > 6) {
            sameMissCount++
        }
        if (sameMissCount > 4) chr.autoban(
            "Autobanned for : $misses Miss godmode",
            1
        ) else if (sameMissCount > 0) lastMisses = misses
        misses = 0
    }

    /**
     * Records the current time as the last spam time for a specific type.
     *
     * This function is used to track the last time a spam action of a specific type was made by the character associated with this autoban manager.
     * The type is an integer that represents the type of action that is considered spam.
     *
     * @param type The type of spam action.
     */
    fun spam(type: Int) {
        spam[type] = System.currentTimeMillis()
    }

    /**
     * Retrieves the last spam time for a specific type.
     *
     * This function is used to get the last time a spam action of a specific type was made by the character associated with this autoban manager.
     * The type is an integer that represents the type of action that is considered spam.
     *
     * @param type The type of spam action.
     * @return The last time a spam action of the specified type was made.
     */
    fun getLastSpam(type: Int) = spam[type]

    /**
     * Timestamp checker
     *
     * `type`:<br></br>
     * 0: HealOverTime<br></br>
     * 1: Pet Food<br></br>
     * 2: ItemSort<br></br>
     * 3: ItemIdSort<br></br>
     * 4: SpecialMove<br></br>
     * 5: UseCatchItem<br></br>
     *
     * @param type type
     * @return Timestamp checker
     */
    fun setTimestamp(type: Int, time: Int) {
        if (timestamp[type] == time) {
            timestampCounter[type]++
            if (timestampCounter[type] > 3) {
                chr.client.disconnect(shutdown = false, cashShop = false)
                //System.out.println("Same timestamp for type: " + type + "; Character: " + chr);
            }
            return
        }
        timestamp[type] = time
    }
}