package net.server.channel.handlers

import client.Client
import client.inventory.Equip
import client.inventory.InventoryType
import client.inventory.ModifyInventory
import net.AbstractPacketHandler
import server.ItemInformationProvider
import tools.data.input.SeekableLittleEndianAccessor
import tools.packet.CharacterPacket
import tools.packet.ItemPacket

class ScrollHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val slot = slea.readShort().toByte()
        val dst = slea.readShort().toByte()
        val toScroll = c.player?.getInventory(InventoryType.EQUIPPED)?.getItem(dst) as Equip
        val oldLevel = toScroll.level
        if (toScroll.upgradeSlots < 1) {
            c.announce(CharacterPacket.getInventoryFull())
            return
        }
        val useInventory = c.player?.getInventory(InventoryType.USE)
        val scroll = useInventory?.getItem(slot) ?: return
        val scrollReqs = ItemInformationProvider.getScrollReqs(scroll.itemId)
        if (scrollReqs.isNotEmpty() && !scrollReqs.contains(toScroll.itemId)) {
            c.announce(CharacterPacket.getInventoryFull())
            return
        }
        if (scroll.itemId != 2049100 && !isCleanState(scroll.itemId)) {
            if (!canScroll(scroll.itemId, toScroll.itemId)) return
        }
        if (scroll.quantity < 1) return
        val scrolled = ItemInformationProvider.scrollEquipWithId(toScroll, scroll.itemId, false, c.player?.isGM() == true) as Equip
        val scrollSuccess = if (scrolled == null) {
            Equip.ScrollResult.CURSE
        } else if (scrolled.level > oldLevel || (isCleanState(scroll.itemId) && scrolled.level.toInt() == oldLevel + 1)) {
            Equip.ScrollResult.SUCCESS
        } else Equip.ScrollResult.FAIL
        useInventory.removeItem(scroll.position, 1, false)
        val mods = mutableListOf<ModifyInventory>()
        if (scrollSuccess == Equip.ScrollResult.CURSE) {
            mods.add(ModifyInventory(3, toScroll))
            if (dst < 0) {
                c.player?.getInventory(InventoryType.EQUIPPED)?.removeItem(toScroll.position)
            } else {
                c.player?.getInventory(InventoryType.EQUIP)?.removeItem(toScroll.position)
            }
        } else {
            mods.add(ModifyInventory(3, scrolled))
            mods.add(ModifyInventory(0, scrolled))
        }
        c.player?.let { player ->
            c.announce(CharacterPacket.modifyInventory(true, mods))
            player.forceUpdateItem(scroll)
            player.let { it.map.broadcastMessage(ItemPacket.getScrollEffect(it.id, scrollSuccess)) }
            if (dst < 0 && (scrollSuccess == Equip.ScrollResult.SUCCESS || scrollSuccess == Equip.ScrollResult.CURSE)) {
                player.equipChanged()
            }
            player.notMovePlayer = true
        }
    }

    private fun isCleanState(scrollId: Int) = scrollId in 2049000..2049003

    private fun canScroll(scrollId: Int, itemId: Int) = (scrollId / 100) % 100 == (itemId / 10000) % 100
}