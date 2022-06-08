package net.server.channel.handlers

import client.Client
import client.inventory.InventoryType
import net.AbstractPacketHandler
import server.InventoryManipulator.Companion.addById
import server.InventoryManipulator.Companion.removeById
import server.ItemInformationProvider
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import tools.packet.GameplayPacket

class UseCatchItemHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        val abm = chr.autoban
        abm.setTimestamp(5, slea.readInt())
        slea.readShort()
        val itemId = slea.readInt()
        val monsterId = slea.readInt()
        val mob = chr.map.getMonsterByOid(monsterId) ?: return
        if ((chr.getInventory(ItemInformationProvider.getInventoryType(itemId))?.countById(itemId) ?: 0) <= 0) {
            return
        }
        when (itemId) {
            2270000 -> {
                if (mob.id == 9300101) {
                    chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                    mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                    removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = true)
                    addById(c, 1902000, 1.toShort(), "", -1)
                }
                c.announce(PacketCreator.enableActions())
            }
            2270001 -> if (mob.id == 9500197) {
                if (abm.getLastSpam(10) + 1000 < System.currentTimeMillis()) {
                    if (mob.hp < mob.getMaxHp() / 10 * 4) {
                        chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                        mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                        removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = true)
                        addById(c, 4031830, 1.toShort(), "", -1)
                    } else {
                        abm.spam(10)
                        c.announce(PacketCreator.catchMessage(0))
                    }
                }
                c.announce(PacketCreator.enableActions())
            }
            2270002 -> if (mob.id == 9300157) {
                if (abm.getLastSpam(10) + 800 < System.currentTimeMillis()) {
                    if (mob.hp < mob.getMaxHp() / 10 * 4) {
                        if (Math.random() < 0.5) { // 50% chance
                            chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                            mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                            removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = true)
                            addById(c, 4031868, 1.toShort(), "", -1)
                        } else {
                            chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 0.toByte()))
                        }
                        abm.spam(10)
                    } else {
                        c.announce(PacketCreator.catchMessage(0))
                    }
                }
                c.announce(PacketCreator.enableActions())
            }
            2270003 -> {
                if (mob.id == 9500320) {
                    if (mob.hp < mob.getMaxHp() / 10 * 4) {
                        chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                        mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                        removeById(c, InventoryType.USE, itemId, 1, true, consume = true)
                        addById(c, 4031887, 1.toShort(), "", -1)
                    } else {
                        c.announce(PacketCreator.catchMessage(0))
                    }
                }
                c.announce(PacketCreator.enableActions())
            }
            2270005 -> {
                if (mob.id == 9300187) {
                    if (mob.hp < mob.getMaxHp() / 10 * 3) {
                        chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                        mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                        removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = true)
                        addById(c, 2109001, 1.toShort(), "", -1)
                    } else {
                        c.announce(PacketCreator.catchMessage(0))
                    }
                }
                c.announce(PacketCreator.enableActions())
            }
            2270006 -> {
                if (mob.id == 9300189) {
                    if (mob.hp < mob.getMaxHp() / 10 * 3) {
                        chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                        mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                        removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = true)
                        addById(c, 2109002, 1.toShort(), "", -1)
                    } else {
                        c.announce(PacketCreator.catchMessage(0))
                    }
                }
                c.announce(PacketCreator.enableActions())
            }
            2270007 -> {
                if (mob.id == 9300191) {
                    if (mob.hp < mob.getMaxHp() / 10 * 3) {
                        chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                        mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                        removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = true)
                        addById(c, 2109003, 1.toShort(), "", -1)
                    } else {
                        c.announce(PacketCreator.catchMessage(0))
                    }
                }
                c.announce(PacketCreator.enableActions())
            }
            2270004 -> {
                if (mob.id == 9300175) {
                    if (mob.hp < mob.getMaxHp() / 10 * 4) {
                        chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                        mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                        removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = true)
                        addById(c, 4001169, 1.toShort(), "", -1)
                    } else {
                        c.announce(PacketCreator.catchMessage(0))
                    }
                }
                c.announce(PacketCreator.enableActions())
            }
            2270008 -> if (mob.id == 9500336) {
                if (abm.getLastSpam(10) + 3000 < System.currentTimeMillis()) {
                    abm.spam(10)
                    chr.map.broadcastMessage(CharacterPacket.catchMonster(monsterId, itemId, 1.toByte()))
                    mob.map?.killMonster(mob, null, withDrops = false, secondTime = false, animation = 1)
                    removeById(c, InventoryType.USE, itemId, 1, fromDrop = true, consume = true)
                    addById(c, 2022323, 1.toShort(), "", -1)
                } else {
                    chr.message("You cannot use the Fishing Net yet.")
                }
                c.announce(PacketCreator.enableActions())
            }
            else -> { }
        }
    }
}