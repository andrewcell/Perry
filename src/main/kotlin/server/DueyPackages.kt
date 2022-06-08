package server

import client.inventory.Item
import java.util.*

class DueyPackages(val packageId: Int, val item: Item? = null) {
    var sender = ""
    private var day = 0
    private var month = 0
    private var year = 0
    var mesos = 0

    fun sentTimeInMilliseconds(): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        return cal.timeInMillis
    }

    fun setSentTime(sentTime: String) {
        day = sentTime.substring(0, 2).toInt()
        month = sentTime.substring(3, 5).toInt()
        year = sentTime.substring(6, 10).toInt()
    }
}