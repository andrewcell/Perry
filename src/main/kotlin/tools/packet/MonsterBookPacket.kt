package tools.packet

import client.Character
import net.SendPacketOpcode
import tools.PacketCreator
import tools.data.output.PacketLittleEndianWriter

class MonsterBookPacket {
    companion object {
        fun addMonsterBookInfo(lew: PacketLittleEndianWriter, chr: Character) {
            lew.int(chr.bookCover)
            lew.byte(0)
            val cards = chr.monsterBook.cards
            lew.short(cards.size)
            cards.forEach { (id, lv) ->
                lew.short(id % 10000)
                lew.byte(lv)
            }
        }

        fun addCard(full: Boolean, cardId: Int, level: Int): ByteArray {
            val lew = PacketLittleEndianWriter(11)
            lew.byte(SendPacketOpcode.MONSTER_BOOK_SET_CARD.value)
            lew.byte(if (full) 0 else 1)
            lew.int(cardId)
            lew.int(level)
            return lew.getPacket()
        }

        fun changeCover(cardId: Int): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.byte(SendPacketOpcode.MONSTER_BOOK_SET_COVER.value)
            lew.int(cardId)
            return lew.getPacket()
        }

        fun showForeignCardEffect(id: Int): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.byte(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.int(id)
            lew.byte(0x0D)
            return lew.getPacket()
        }

        fun showGainCard(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.byte(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.byte(0x0D)
            return lew.getPacket()
        }

        fun showMonsterBookPickup() = PacketCreator.showSpecialEffect(14)
    }
}