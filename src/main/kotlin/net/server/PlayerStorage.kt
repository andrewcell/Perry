package net.server

import client.Character
import java.util.concurrent.locks.ReentrantReadWriteLock

class PlayerStorage {
    private val locks = ReentrantReadWriteLock()
    val storage = mutableMapOf<Int, Character>()
    private val rLock = locks.readLock()
    private val wLock = locks.writeLock()

    fun addPlayer(chr: Character) {
        wLock.lock()
        try {
            storage[chr.id] = chr
        } finally {
            wLock.unlock()
        }
    }

    fun removePlayer(chr: Int): Character? {
        wLock.lock()
        try {
            return storage.remove(chr)
        } finally {
            wLock.unlock()
        }
    }

    fun getCharacterByName(name: String): Character? {
        rLock.lock()
        try {
            return storage.values.find { it.name.equals(name, true) }
        } finally {
            rLock.unlock()
        }
    }

    fun getCharacterById(id: Int): Character? {
        rLock.lock()
        try {
            return storage[id]
        } finally {
            rLock.unlock()
        }
    }

    fun getAllCharacters(): List<Character> {
        rLock.lock()
        try {
            return storage.values.toList()
        } finally {
            rLock.unlock()
        }
    }

    fun disconnectAll() {
        wLock.lock()
        try {
            storage.values.forEach {
                it.client.disconnect(shutdown = true, cashShop = false)
            }
            storage.clear()
        } finally {
            wLock.unlock()
        }
    }

    fun saveAll() {
        wLock.lock()
        try {
            storage.values.forEach { it.client.disconnect(shutdown = true, cashShop = false) }
        } finally {
            wLock.unlock()
        }
    }
}