package tools.packet

import client.Character
import net.SendPacketOpcode
import tools.PacketCreator
import tools.PacketCreator.Companion.packetWriter
import tools.data.output.PacketLittleEndianWriter

class MonsterBookPacket {
    companion object {
        /*fun addMonsterBookInfo(chr: Character): PacketLittleEndianWriter {
            lew.int(chr.bookCover)
            lew.byte(0)
            val cards = chr.monsterBook.cards
            lew.short(cards.size)
            cards.forEach { (id, lv) ->
                lew.short(id % 10000)
                lew.byte(lv)
            }
        }*/

        fun addCard(full: Boolean, cardId: Int, level: Int) = packetWriter(SendPacketOpcode.MONSTER_BOOK_SET_CARD, 11) {
            byte(if (full) 0 else 1)
            int(cardId)
            int(level)
        }

        fun changeCover(cardId: Int) = packetWriter(SendPacketOpcode.MONSTER_BOOK_SET_COVER, 6) {
            int(cardId)
        }

        fun showForeignCardEffect(id: Int) = packetWriter(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT, 7) {
            int(id)
            byte(0x0D)
        }

        fun showGainCard() = packetWriter(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT, 3) {
            byte(0x0D)
        }

        fun showMonsterBookPickup() = PacketCreator.showSpecialEffect(14)
    }
}