package server

import client.inventory.Item
import java.util.Calendar

data class MTSItemInfo(
    val item: Item,
    val price: Int,
    val id: Int,
    val cid: Int,
    val seller: String,
    val date: String
) {
    private val year = date.substring(0, 4).toInt()
    private val month = date.substring(5, 7).toInt()
    private val day = date.substring(8, 10).toInt()

    fun getTaxes() = 100 + price / 10

    fun getEndingDate(): Long {
        val now = Calendar.getInstance()
        now.set(year, month - 1, day)
        return now.timeInMillis
    }
}
