package net.server.handlers.login

import client.Character
import client.Client
import client.GameJob
import client.inventory.InventoryType
import client.inventory.Item
import net.AbstractPacketHandler
import server.ItemInformationProvider
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket

class CreateCharHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val name = slea.readGameASCIIString()
        if (!Character.checkNameAvailable(name)) return
        val newChar = Character.getDefault(c)
        val face = slea.readInt()
        val hair = slea.readInt()
        var top = slea.readInt()
        var bottom = slea.readInt()
        val shoes = slea.readInt()
        var weapon = slea.readInt()
        val str = slea.readByte().toShort()
        val dex = slea.readByte().toShort()
        val int = slea.readByte().toShort()
        val luk = slea.readByte().toShort()
        newChar.world = c.world
        newChar.name = name
        newChar.gender = c.gender.toInt()
        newChar.face = face
        newChar.hair = hair
        newChar.str = str.toInt()
        newChar.dex = dex.toInt()
        newChar.int = int.toInt()
        newChar.luk = luk.toInt()
        newChar.job = GameJob.BEGINNER
        newChar.mapId = 0
        newChar.getInventory(InventoryType.ETC)?.addItem(Item(4161001, 0.toByte(), 1.toShort()))
        //newChar.getInventory(InventoryType.CASH).addItem(new Item(5072000, (byte) 0, (short) 30));
        // Check for equips
        val equip = newChar.getInventory(InventoryType.EQUIPPED)
        if (newChar.isGM()) {
            val eqHat = ItemInformationProvider.getEquipById(1002140)
            eqHat.position = -1
            equip?.addFromDatabase(eqHat)
            top = 1042003
            bottom = 1062007
            weapon = 1322013
        }
        val eqTop = ItemInformationProvider.getEquipById(top)
        eqTop.position = -5
        equip?.addFromDatabase(eqTop)
        val eqBottom = ItemInformationProvider.getEquipById(bottom)
        eqBottom.position = -6
        equip?.addFromDatabase(eqBottom)
        val eqShoes = ItemInformationProvider.getEquipById(shoes)
        eqShoes.position = -7
        equip?.addFromDatabase(eqShoes)
        val eqWeapon = ItemInformationProvider.getEquipById(weapon)
        eqWeapon.position = -11
        equip?.addFromDatabase(eqWeapon.copy())
        if (!newChar.insertNewChar()) {
            c.announce(CharacterPacket.deleteCharResponse(0, 0))
            return
        }
        c.announce(CharacterPacket.addNewCharEntry(newChar))
    }
}