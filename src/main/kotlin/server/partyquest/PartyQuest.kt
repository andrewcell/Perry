package server.partyquest

import client.Character
import net.server.Server
import net.server.world.Party

open class PartyQuest(val party: Party) {
    val channel = party.leader.channel ?: -1
    val world = party.leader.world ?: -1
    val participants: MutableList<Character> = mutableListOf()

    init {
        val mapId = party.leader.mapId
        for (pChr in party.members) {
            if (pChr.channel == channel && pChr.mapId == mapId) {
                val chr = pChr.id?.let { Server.getWorld(world).channels[channel].players.getCharacterById(it) }
                if (chr != null) participants.add(chr)
            }
        }
    }

    @Throws(Throwable::class)
    fun removeParticipant(chr: Character) {
        synchronized(participants) {
            participants.remove(chr)
            chr.partyQuest = null
        }
    }
}