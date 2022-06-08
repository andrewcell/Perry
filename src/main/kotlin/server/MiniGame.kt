package server

import client.Character
import client.Client
import server.maps.AbstractMapObject
import server.maps.MapObjectType
import tools.packet.InteractPacket
import tools.packet.MiniGamePacket

class MiniGame(val owner: Character, val description: String, val locker: Int, val password: String?) : AbstractMapObject() {
    var visitor: Character? = null
    private val list4x3 = mutableListOf<Int>()
    private val list5x4 = mutableListOf<Int>()
    private val list6x5 = mutableListOf<Int>()
    private val piece = arrayOfNulls<Int>(250)
    var pieceType = -1
    var loser = 1
    var gameType: String? = null
        set(value) {
            field = value
            if (value == "matchcard") {
                when (matchesToWin) {
                    6 -> {
                        for (i in 0..5) {
                            list4x3.add(i)
                            list4x3.add(i)
                        }
                    }
                    10 -> {
                        for (i in 0..9) {
                            list5x4.add(i)
                            list5x4.add(i)
                        }
                    }
                    else -> {
                        for (i in 0..14) {
                            list6x5.add(i)
                            list6x5.add(i)
                        }
                    }
                }
            }
        }
    var firstSlot = 0
    private var visitorPoints = 0
    private var ownerPoints = 0
    var matchesToWin = 0

    fun hasFreeSlot() = visitor == null

    fun isOwner(chr: Character?) = owner == chr

    fun isVisitor(chr: Character) = visitor == chr

    fun addVisitor(visitor: Character) {
        this.visitor = visitor
        when (gameType) {
            "omok" -> {
                owner.client.announce(MiniGamePacket.getMiniGameNewVisitor(visitor, 1))
                owner.map.broadcastMessage(MiniGamePacket.addOmokBox(owner, locker, 2, 0))
            }
            "matchcard" -> {
                owner.client.announce(MiniGamePacket.getMatchCardNewVisitor(visitor, 1))
                owner.map.broadcastMessage(MiniGamePacket.addMatchCardBox(owner, locker, 2, 0))
            }
            else -> return
        }
    }

    fun removeVisitor(visitor: Character) {
        if (this.visitor == visitor) {
           this.visitor = null
            owner.client.announce(MiniGamePacket.getMiniGameRemoveVisitor())
            when (gameType) {
                "omok" -> owner.map.broadcastMessage(MiniGamePacket.addOmokBox(owner, 0, 1, 0))
                "matchcard" -> owner.map.broadcastMessage(MiniGamePacket.addMatchCardBox(owner, 0, 1, 0))
                else -> return
            }
        }
    }

    fun banVisitor() {
        visitor?.client?.announce(MiniGamePacket.getMiniGameClose(false))
        visitor = null
        owner.client.announce(MiniGamePacket.getMiniGameRemoveVisitor())
        when (gameType) {
            "omok" -> owner.map.broadcastMessage(MiniGamePacket.addOmokBox(owner, 0, 1, 0))
            "matchcard" -> owner.map.broadcastMessage(MiniGamePacket.addMatchCardBox(owner, 0, 1, 0))
            else -> return
        }
    }

    fun setOwnerPoints() {
        ownerPoints++
        if (ownerPoints + visitorPoints == matchesToWin) {
            if (ownerPoints == visitorPoints) {
                broadcast(MiniGamePacket.getMatchCardTie(this))
            } else if (ownerPoints > visitorPoints) {
                broadcast(MiniGamePacket.getMatchCardOwnerWin(this))
            } else {
                broadcast(MiniGamePacket.getMatchCardVisitorWin(this))
            }
            ownerPoints = 0
            visitorPoints = 0
        }
    }

    fun setVisitorPoints() {
        visitorPoints++
        if (ownerPoints + visitorPoints == matchesToWin) {
            if (ownerPoints > visitorPoints) {
                broadcast(MiniGamePacket.getMiniGameOwnerWin(this))
            } else if (visitorPoints > ownerPoints) {
                broadcast(MiniGamePacket.getMiniGameVisitorWin(this))
            } else {
                broadcast(MiniGamePacket.getMiniGameTie(this))
            }
            ownerPoints = 0
            visitorPoints = 0
        }
    }

    fun shuffleList() {
        when (matchesToWin) {
            6 -> list4x3.shuffle()
            10 -> list5x4.shuffle()
            else -> list6x5.shuffle()
        }
    }

    fun getCardId(slot: Int) = when (matchesToWin) {
        6 -> list4x3[slot - 1]
        10 -> list5x4[slot - 1]
        else -> list6x5[slot - 1]
    }

    fun broadcast(packet: ByteArray) {
        owner.client.announce(packet)
        visitor?.client?.announce(packet)
    }

    fun chat(c: Client, chat: String) = broadcast(InteractPacket.getPlayerShopChat(c.player, chat, isOwner(c.player)))

    fun sendOmok(c: Client, type: Int) = c.announce(MiniGamePacket.getMiniGame(c, this, isOwner(c.player), type))

    fun sendMatchCard(c: Client, type: Int) = c.announce(MiniGamePacket.getMatchCard(c, this, isOwner(c.player), type))

    private fun searchCombo(x: Int, y: Int, type: Int): Boolean {
        val slot = y * 15 + x + 1
        for (i in 0..4) {
            if (piece[slot + i] == type) {
                if (i == 4) {
                    return true
                }
            } else {
                break
            }
        }
        for (j in 15..16) {
            for (i in 0..4) {
                if (piece[slot + i * j] == type) {
                    if (i == 4) {
                        return true
                    }
                } else {
                    break
                }
            }
        }
        return false
    }

    private fun searchCombo2(x: Int, y: Int, type: Int): Boolean {
        val slot = y * 15 + x + 1
        for (j in 14..14) {
            for (i in 0..4) {
                if (piece[slot + i * j] == type) {
                    if (i == 4) {
                        return true
                    }
                } else {
                    break
                }
            }
        }
        return false
    }

    fun setPiece(move1: Int, move2: Int, type: Int, chr: Character) {
        val slot = move2 * 15 + move1 + 1
        if (piece[slot] == 0) {
            piece[slot] = type
            broadcast(MiniGamePacket.getMiniGameMoveOmok(this, move1, move2, type))
            for (y in 0..14) {
                for (x in 0..10) {
                    if (searchCombo(x, y, type)) {
                        loser = if (owner == chr) {
                            broadcast(MiniGamePacket.getMiniGameOwnerWin(this))
                            0
                        } else {
                            broadcast(MiniGamePacket.getMiniGameVisitorWin(this))
                            1
                        }
                        for (y2 in 0..14) {
                            for (x2 in 0..14) {
                                val slot2 = (y2 * 15 + x2 + 1)
                                piece[slot2] = 0
                            }
                        }
                    }
                }
            }
            for (y in 0..14) {
                for (x in 4..14) {
                    if (searchCombo2(x, y, type)) {
                        loser = if (owner == chr) {
                            broadcast(MiniGamePacket.getMiniGameOwnerWin(this))
                            0
                        } else {
                            broadcast(MiniGamePacket.getMiniGameVisitorWin(this))
                            1
                        }
                        for (y2 in 0..14) {
                            for (x2 in 0..14) {
                                val slot2 = y2 * 15 + x2 + 1
                                piece[slot2] = 0
                            }
                        }
                    }
                }
            }

        }
    }

    override val objectType: MapObjectType = MapObjectType.MINI_GAME

    override fun sendDestroyData(client: Client) {}

    override fun sendSpawnData(client: Client) {}
}