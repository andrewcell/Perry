package server.maps

abstract class AbstractLoadedLife(val id: Int) : AbstractAnimatedMapObject() {
    var f: Int = 0
    var hidden = false
    var fh: Int? = null
    private var startFh: Int? = null
    var cy: Int = 0
    var rx0: Int = 0
    var rx1: Int = 0
    var cType: String? = null
    var mTime: Int = 0

    constructor(life: AbstractLoadedLife) : this(life.id) {
        f = life.f
        hidden = life.hidden
        fh = life.fh
        startFh = life.startFh
        cy = life.cy
        rx0 = life.rx0
        rx1 = life.rx1
    }
}