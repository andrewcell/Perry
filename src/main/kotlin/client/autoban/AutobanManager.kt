package client.autoban

import client.Character

class AutobanManager(val chr: Character) {
    val points = mutableMapOf<AutobanFactory, Int>()
    val lastTime = mutableMapOf<AutobanFactory, Long>()
    var misses = 0
    var lastMisses = 0
    var sameMissCount = 0
    var spam = LongArray(20)
    var timestamp = IntArray(20)
    var timestampCounter = ByteArray(20)

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

    fun addMiss() = misses++

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

    //Don't use the same type for more than 1 thing
    fun spam(type: Int) {
        spam[type] = System.currentTimeMillis()
    }

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