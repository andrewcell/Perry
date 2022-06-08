package net.server.world

class Messenger(val id: Int, chr: MessengerCharacter) {
    val members = mutableListOf(chr)
    val pos = arrayOfNulls<Boolean>(3)

    init {
        chr.position = getLowestPosition(true)
    }

    fun addMember(member: MessengerCharacter) {
        members.add(member)
        member.position = getLowestPosition(true)
    }

    fun removeMember(member: MessengerCharacter, position: Int) {
        pos[position] = false
        members.remove(member)
    }

    fun silentRemoveMember(member: MessengerCharacter) = members.remove(member)

    fun silentAddMember(member: MessengerCharacter, position: Int) {
        members.add(member)
        member.position = position
    }

    fun getPositionByName(name: String) = members.find { it.name == name }?.position ?: 4

    fun getLowestPosition(set: Boolean = false): Int {
        for (b in 0..2) {
            if (pos[b] != false) {
                if (set) pos[b] = true
                return b
            }
        }
        return -1
    }
}