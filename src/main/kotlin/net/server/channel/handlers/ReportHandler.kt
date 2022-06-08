package net.server.channel.handlers

import client.Character
import client.Client
import database.Reports
import mu.KLogging
import net.AbstractPacketHandler
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.InteractPacket
import java.sql.SQLException

class ReportHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val type = slea.readByte() // 01 = Conversation claim, 00 = illegal program
        val victim = slea.readGameASCIIString()
        val reason = slea.readByte()
        val description = slea.readGameASCIIString()
        c.player?.let { player ->
            when (type.toInt()) {
                0 -> {
                    if (player.possibleReports > 0) {
                        if (player.meso.get() > 299) {
                            player.decreaseReports()
                            player.gainMeso(-300, true)
                        } else {
                            c.announce(PacketCreator.reportResponse(4))
                            return
                        }
                    } else {
                        c.announce(PacketCreator.reportResponse(2))
                        return
                    }
                    c.getChannelServer().broadcastGMPacket(InteractPacket.serverNotice(6, "$victim was reported for $description."))
                    addReport(player.id, Character.getIdByName(victim), 0, description, null)
                }
                1 -> {
                    val chatLog = slea.readGameASCIIString()
                    if (player.possibleReports > 0) {
                        if (player.meso.get() > 299) {
                            player.decreaseReports()
                            player.gainMeso(-300, true)
                        } else {
                            c.announce(PacketCreator.reportResponse(4))
                            return
                        }
                    }
                    c.getChannelServer().broadcastGMPacket(InteractPacket.serverNotice(6, "$victim was reported for $description."))
                    addReport(player.id, Character.getIdByName(victim), reason.toInt(), description, chatLog)
                }
                else -> {
                    c.getChannelServer().broadcastGMPacket(InteractPacket.serverNotice(6, "Possible packet editing detected. ${player.name}. Unknown report type."))
                }
            }
        }
    }

    private fun addReport(reporterId: Int, victimId: Int, reason: Int, description: String, chatLog: String?) {
        try {
            transaction {
                Reports.insert {
                    it[id] = reporterId
                    it[Reports.victimId] = victimId
                    it[Reports.reason] = reason.toByte()
                    it[Reports.description] = description
                    it[Reports.chatLog] = chatLog ?: ""
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Failed to add report to database." }
        }
    }

    companion object : KLogging()
}