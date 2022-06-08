package tools.packet

import client.Character
import net.SendPacketOpcode
import tools.PacketCreator
import tools.data.output.PacketLittleEndianWriter

class MonsterBookPacket {
    companion object {
        fun addMonsterBookInfo(lew: PacketLittleEndianWriter, chr: Character) {
            lew.writeInt(chr.bookCover)
            lew.write(0)
            val cards = chr.monsterBook.cards
            lew.writeShort(cards.size)
            cards.forEach { (id, lv) ->
                lew.writeShort(id % 10000)
                lew.write(lv)
            }
        }

        fun addCard(full: Boolean, cardId: Int, level: Int): ByteArray {
            val lew = PacketLittleEndianWriter(11)
            lew.write(SendPacketOpcode.MONSTER_BOOK_SET_CARD.value)
            lew.write(if (full) 0 else 1)
            lew.writeInt(cardId)
            lew.writeInt(level)
            return lew.getPacket()
        }

        fun changeCover(cardId: Int): ByteArray {
            val lew = PacketLittleEndianWriter(6)
            lew.write(SendPacketOpcode.MONSTER_BOOK_SET_COVER.value)
            lew.writeInt(cardId)
            return lew.getPacket()
        }

        fun showForeignCardEffect(id: Int): ByteArray {
            val lew = PacketLittleEndianWriter(7)
            lew.write(SendPacketOpcode.SHOW_FOREIGN_EFFECT.value)
            lew.writeInt(id)
            lew.write(0x0D)
            return lew.getPacket()
        }

        fun showGainCard(): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.write(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value)
            lew.write(0x0D)
            return lew.getPacket()
        }

        fun showMonsterBookPickup() = PacketCreator.showSpecialEffect(14)
    }
}