package server.events.gm

class Coconuts(val id: Int) {
    var hits = 0
    var isHittable = false
    var hitTime = System.currentTimeMillis()

    fun hit() {
        hitTime = System.currentTimeMillis() + 750
        hits++
    }

    fun resetHits() {
        hits = 0
    }
}