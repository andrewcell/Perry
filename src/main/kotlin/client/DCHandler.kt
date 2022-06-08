package client

import net.AbstractPacketHandler
import tools.data.input.SeekableLittleEndianAccessor

class DCHandler : AbstractPacketHandler() {
    override fun handlePacket(slea: SeekableLittleEndianAccessor, c: Client) {
        c.dcConnect()
    }
}