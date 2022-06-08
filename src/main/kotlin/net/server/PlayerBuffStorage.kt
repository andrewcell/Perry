package net.server

import java.util.concurrent.locks.ReentrantLock

class PlayerBuffStorage {
    val id = Math.random() * 100
    private val mutex = ReentrantLock()
    private val buffs = mutableMapOf<Int, List<PlayerBuffValueHolder>>()

    fun addBuffsToStorage(cid: Int, toStore: List<PlayerBuffValueHolder>) {
        mutex.lock()
        try {
            buffs[cid] = toStore
        } finally {
            mutex.unlock()
        }
    }

    fun getBuffsFromStorage(cid: Int): List<PlayerBuffValueHolder> {
        mutex.lock()
        try {
            return buffs.remove(cid) ?: listOf()
        } finally {
            mutex.unlock()
        }
    }
}