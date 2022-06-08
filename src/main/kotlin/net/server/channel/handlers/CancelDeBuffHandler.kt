package net.server.channel.handlers

import client.Client
import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class CancelDeBuffHandler : AbstractPacketHandler() {
    //TIP: BAD STUFF LOL!
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        /*List<Disease> diseases = c.getPlayer().getDiseases();
         List<Disease> diseases_ = new ArrayList<Disease>();
         for (Disease disease : diseases) {
         List<Disease> disease_ = new ArrayList<Disease>();
         disease_.add(disease);
         diseases_.add(disease);
         c.announce(PacketCreator.cancelDeBuff(disease_));
         c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.cancelForeignDeBuff(c.getPlayer().getId(), disease_), false);
         }
         for (Disease disease : diseases_) {
         c.getPlayer().removeDisease(disease);
         }*/
    }
}