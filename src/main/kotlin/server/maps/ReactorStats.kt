package server.maps

import java.awt.Point

class ReactorStats {
    var tl: Point? = null
    var br: Point? = null
    private val stateInfo = mutableMapOf<Byte, List<StateData>>()

    fun getNextState(state: Byte, index: Int): Byte {
        val nextState = stateInfo[state] ?: return -1
        if (nextState.size < index + 1) return -1
        return nextState[index].nextState
    }

    fun addState(state: Byte, data: List<StateData>) {
        stateInfo[state] = data
    }

    fun getStateSize(state: Byte) = stateInfo[state]?.size?.toByte() ?: 0

    fun getActiveSkills(state: Byte, index: Int) = stateInfo[state]?.get(index)?.activeSkills

    fun getType(state: Byte): Int = stateInfo[state]?.get(0)?.type ?: -1

    fun getReactItem(state: Byte, index: Int) = stateInfo[state]?.get(index)?.reactItem

    companion object {
        data class StateData(
            val type: Int,
            val reactItem: Pair<Int, Int>?,
            val activeSkills: List<Int>?,
            val nextState: Byte
        )

    }
}