package net.server.channel.handlers

import client.Client
import client.SkillFactory
import client.inventory.InventoryType
import client.inventory.Pet
import client.inventory.PetDataFactory
import database.Pets
import mu.KLoggable
import net.AbstractPacketHandler
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import provider.DataProviderFactory
import provider.DataTool
import server.InventoryManipulator
import tools.PacketCreator
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CashPacket
import java.io.File
import java.sql.SQLException

class SpawnPetHandler : AbstractPacketHandler(), KLoggable {
    override val logger = logger()

    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val chr = c.player ?: return
        val slot = slea.readByte()
        slea.readByte()// == 1.toByte()
        val petItem = chr.getInventory(InventoryType.CASH)?.getItem(slot) ?: return
        val pet = petItem.pet ?: return
        val petId = pet.itemId
        if (petId == 5000028 || petId == 5000047) { // Handles Dragon and robo
            if (!chr.haveItem(petId + 1)) {
                val evolveId = DataTool.getInt("info/evol1", dataRoot.getData("Pet/$petId.img"))
                val petId1 = Pet.createPet(evolveId)
                if (petId1 == -1) return
                try {
                    transaction {
                        Pets.deleteWhere { Pets.id eq pet.uniqueId }
                    }
                } catch (e: SQLException) {
                    logger.error(e) { "Failed to delete pet from database. PetId: ${pet.uniqueId}" }
                }
                val expiration = chr.getInventory(InventoryType.CASH)?.getItem(slot)?.expiration ?: 1
                InventoryManipulator.removeById(c, InventoryType.CASH, petId1, 1, fromDrop = false, consume = false)
                InventoryManipulator.addById(c, evolveId, 1, null, petId1, expiration)
                c.announce(PacketCreator.enableActions())
                return
            } else {
                chr.dropMessage(5, "You can't hatch your ${if (petId == 5000028) "Dragon egg" else "Robo egg"} if you already have a baby ${if (petId == 5000028) "Dragon." else "Robo."}")
                c.announce(PacketCreator.enableActions())
                return
            }
        }
        if (chr.pet != null) {
            chr.unEquipPet(true)
        } else {
            if (SkillFactory.getSkill(8)?.let { chr.getSkillLevel(it).toInt() } == 0 && chr.pet != null) {
                chr.unEquipPet(false)
            }
            //if (lead) chr.shiftPetsRight()
            val pos = chr.position
            pos.y -= 12
            pet.pos = pos
            pet.fh = pet.pos?.let { chr.map.footholds?.findBelow(it)?.id } ?: return
            pet.stance = 0
            pet.summoned = true
            pet.saveToDatabase()
            chr.pet = pet
            c.player?.let { chr.map.broadcastMessage(it, CashPacket.showPet(it, pet, remove = false, hunger = false), true) }
            c.announce(CashPacket.petStatUpdate(c.player))
            chr.forceUpdateItem(petItem)
            chr.startFullnessSchedule(PetDataFactory.getHunger(pet.itemId), pet)
        }
    }

    companion object {
        val dataRoot = DataProviderFactory.getDataProvider(File("${tools.ServerJSON.settings.wzPath}/Item.wz/"))
    }
}