package net.server.world

class Party(val id: Int, var leader: PartyCharacter) {
    val members = mutableListOf(leader)

    fun containsMembers(member: PartyCharacter?) = members.contains(member)

    fun addMember(member: PartyCharacter) = members.add(member)

    fun removeMember(member: PartyCharacter) = members.remove(member)

    fun updateMember(member: PartyCharacter) {
        for (i in members.indices) {
            if (members[i] == member) members[i] = member
        }
    }

    fun getMemberById(id: Int) = members.find { it.id == id }
}