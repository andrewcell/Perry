package net.server.channel.handlers

import client.Client
import database.Notes
import mu.KLogging
import net.AbstractPacketHandler
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket
import tools.packet.InteractPacket
import java.sql.SQLException

class NoteActionHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val action = slea.readByte().toInt()
        c.player?.let { player ->
            if (action == 0 && (player.cashShop?.notes ?: 0) > 0) {
                val charName = slea.readGameASCIIString()
                val message = slea.readGameASCIIString()
                if (player.cashShop?.opened == true) {
                    c.announce(CashPacket.showLocker(c))
                }
                player.sendNote(charName, message, 1)
                player.cashShop?.decreaseNotes()
            } else if (action == 1) {
                val num = slea.readByte()
                slea.readByte()
                slea.readByte()
                var fame = 0
                for (i in 0 until num) {
                    val id = slea.readInt()
                    slea.readByte() // skip fame
                    try {
                        transaction {
                            val row = Notes.select(Notes.fame).where { (Notes.id eq id) and (Notes.deleted eq 0) }
                            if (!row.empty()) {
                                fame += row.first()[Notes.fame]
                            }
                            Notes.update({ Notes.id eq id }) {
                                it[deleted] = 1
                            }
                        }
                    } catch (e: SQLException) {
                        logger.error(e) { "Failed to get notes from database. NoteId: $id" }
                    }
                }
                if (fame > 0) {
                    player.gainFame(fame)
                    c.announce(InteractPacket.getShowFameGain(fame))
                } else return
            } else {
                return
            }
        }
    }

    companion object : KLogging()
}