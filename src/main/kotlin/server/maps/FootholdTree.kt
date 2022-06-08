package server.maps

import java.awt.Point
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos

class FootholdTree(private val p1: Point, private val p2: Point, private val depth: Int = 0) {
    private val center = Point((p2.x - p1.x ) / 2, (p2.y - p1.y) / 2)
    private val footholds = mutableListOf<Foothold>()
    private var nw: FootholdTree? = null
    private var ne: FootholdTree? = null
    private var sw: FootholdTree? = null
    private var se: FootholdTree? = null
    private var maxDropX = -1
    private var minDropX = -1
    
    fun insert(f: Foothold) {
        if (depth == 0) {
            if (f.p1.x > maxDropX) {
                maxDropX = f.p1.x
            } 
            if (f.p1.x < minDropX){
                minDropX = f.p1.x
            }
            if (f.p2.x > maxDropX) {
                maxDropX = f.p2.x
            }
            if (f.p2.x < minDropX) {
                minDropX = f.p2.x
            }
        }
        if (depth == maxDepth || (f.p1.x >= p1.x && f.p2.x <= p2.x && f.p1.y >= p1.y && f.p2.y <= p2.y)) {
            footholds.add(f)
        } else {
            if (nw == null) {
                nw = FootholdTree(p1, center, depth + 1)
                ne = FootholdTree(Point(center.x, p1.y), Point(p2.x, center.y), depth + 1)
                sw = FootholdTree(Point(p1.x, center.y), Point(center.x, p2.y), depth + 1)
                se = FootholdTree(center, p2, depth + 1)
            }
            when {
                (f.p2.x <= center.x && f.p2.y <= center.y) -> nw?.insert(f)
                (f.p1.x > center.x && f.p2.y <= center.y) -> ne?.insert(f)
                (f.p2.x <= center.x && f.p1.y > center.y) -> sw?.insert(f)
                else -> se?.insert(f)
            }
        }
    }
    
    private fun getRelevant(p: Point, list: MutableList<Foothold> = mutableListOf()): List<Foothold> {
        list.addAll(footholds)
        if (nw != null) {
            when {
                (p.x <= center.x && p.y <= center.y) -> nw?.getRelevant(p, list)
                (p.x > center.x && p.y <= center.y) -> ne?.getRelevant(p, list)
                (p.x <= center.x && p.y > center.y) -> sw?.getRelevant(p, list)
                else -> se?.getRelevant(p, list)
            }
        }
        return list
    }

    private fun findWallR(p1: Point, p2: Point): Foothold? {
        footholds.forEach {
            if (it.isWall() && it.p1.x >= p1.x && it.p1.x <= p2.x && it.p1.y >= p1.y && it.p2.y <= p1.y)
                return it
        }
        if (nw != null) {
            return when {
                (p1.x <= center.x && p1.y <= center.y) -> nw?.findWallR(p1, p2)
                ((p1.x > center.x || p2.x > center.x) && p1.y <= center.y) -> ne?.findWallR(p1, p2)
                (p1.x <= center.x && p1.y > center.y) -> sw?.findWallR(p1, p2)
                ((p1.x > center.x || p2.x > center.x) && p1.y > center.y) -> se?.findWallR(p1, p2)
                else -> null
            }
        }
        return null
    }

    fun findWall(p1: Point, p2: Point) = if (p1.y == p2.y) findWallR(p1, p2) else null

    fun findBelow(p: Point): Foothold? {
        val relevant = getRelevant(p)
        val xMatches = mutableListOf<Foothold>()
        relevant.forEach {
            if (it.p1.x <= p.x && it.p2.x >= p.x)
                xMatches.add(it)
        }
        xMatches.sort()
        xMatches.forEach {
            if (!it.isWall() && it.p1.y != it.p2.y) {
                val s1 = abs(it.p2.y - it.p1.y)
                val s2 = abs(it.p2.x - it.p1.x)
                val s4 = abs(p.x - it.p1.x)
                val alpha = atan(s2 / s1.toDouble())
                val beta = atan(s1 / s2.toDouble())
                val s5 = cos(alpha) * (s4 / cos(beta))
                val calcY = if (it.p2.y < it.p1.y) it.p1.y - s5 else it.p1.y + s5
                if (calcY >= p.y) return it
            } else if (!it. isWall()) {
                if (it.p1.y >= p.y) return it
            }
        }
        return null
    }

    companion object {
        const val maxDepth = 8
    }
}