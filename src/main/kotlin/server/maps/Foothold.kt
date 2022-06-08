package server.maps

import java.awt.Point

class Foothold(val p1: Point, val p2: Point, val id: Int) : Comparable<Foothold> {
    var next: Int? = null
    var prev: Int? = null

    fun isWall() = p1.x == p2.x

    private fun getY1() = p1.y

    private fun getY2() = p2.y

    override fun compareTo(other: Foothold): Int {
        return if (p2.y < other.getY1()) -1
        else if (p1.y > other.getY2()) 1
        else 0
    }
}