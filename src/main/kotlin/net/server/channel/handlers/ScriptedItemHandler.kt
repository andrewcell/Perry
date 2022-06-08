package net.server.channel.handlers

import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor
import client.Client
import server.ItemInformationProvider
import scripting.item.ItemScriptManager

class ScriptedItemHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        val ii = ItemInformationProvider
        slea.readInt() // trash stamp
        val itemSlot = slea.readShort().toByte() // item slot
        val itemId = slea.readInt() // itemId
        val (_, script) = ii.getScriptedItemInfo(itemId) ?: return
        val item = c.player?.getInventory(ii.getInventoryType(itemId))?.getItem(itemSlot)
        if (item == null || item.itemId != itemId || item.quantity < 1 || !ItemScriptManager.scriptExists(script)) {
            return
        }
        ItemScriptManager.getItemScript(c, script)
        //NPCScriptManager.getInstance().start(c, info.getNpc(), null, null);        
    }
}