package tools.packet

import client.Client
import constants.ItemConstants
import net.SendPacketOpcode
import server.ItemInformationProvider
import server.ShopItem
import server.life.Npc
import tools.HexTool
import tools.PacketCreator
import tools.data.output.PacketLittleEndianWriter

class NpcPacket {
    companion object {
        fun getNpcShop(c: Client, sid: Int, items: List<ShopItem>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.OPEN_NPC_SHOP.value)
            lew.writeInt(sid)
            //lew.writeShort(items.size() - 27); // 0x26
            lew.writeShort(items.size) // 0x26
            for ((buyable, itemId, price) in items) {
                /*if(ItemConstants.isRechargable(item.getItemId())) {
                continue;
            }*/
                lew.writeInt(itemId)
                lew.writeInt(price)
                if (!ItemConstants.isRechargeable(itemId)) {
                    lew.writeShort(buyable.toInt())
                } else {
                    //lew.writeShort(0);
                    lew.writeInt(0)
                    lew.writeShort(0)
                    lew.writeShort(PacketCreator.doubleToShortBits(ItemInformationProvider.getPrice(itemId)))
                    lew.writeShort(ItemInformationProvider.getSlotMax(c, itemId).toInt())
                }
            }
            return lew.getPacket()
        }

        /**
         * Possible values for
         * `speaker`:<br></br> 0: Npc talking (left)<br></br> 1: Npc talking
         * (right)<br></br> 2: Player talking (left)<br></br> 3: Player talking (left)<br></br>
         *
         * @param npc      Npcid
         * @param msgType
         * @param talk
         * @param endBytes
         * @return
         */
        fun getNpcTalk(npc: Int, msgType: Byte, talk: String, endBytes: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.NPC_TALK.value)
            lew.write(4) // ?
            lew.writeInt(npc)
            lew.write(msgType)
            lew.writeGameASCIIString(talk)
            lew.write(HexTool.getByteArrayFromHexString(endBytes))
            return lew.getPacket()
        }

        fun getNpcTalkNum(npc: Int, talk: String, def: Int, min: Int, max: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.NPC_TALK.value)
            lew.write(4) // ?
            lew.writeInt(npc)
            lew.write(3)
            //lew.write(0); //speaker
            lew.writeGameASCIIString(talk)
            lew.writeInt(def)
            lew.writeInt(min)
            lew.writeInt(max)
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun getNpcTalkStyle(npc: Int, talk: String, styles: Array<Int>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.NPC_TALK.value)
            lew.write(4) // ?
            lew.writeInt(npc)
            lew.write(5)
            //lew.write(0); //speaker
            lew.writeGameASCIIString(talk)
            lew.write(styles.size)
            for (i in styles.indices) {
                lew.writeInt(styles[i])
            }
            return lew.getPacket()
        }

        fun getNpcTalkText(npc: Int, talk: String, def: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.NPC_TALK.value)
            lew.write(4) // Doesn't matter
            lew.writeInt(npc)
            lew.write(2)
            //lew.write(0); //speaker
            lew.writeGameASCIIString(talk)
            lew.writeGameASCIIString(def) //:D
            lew.writeInt(0)
            return lew.getPacket()
        }

        fun removeNpc(oid: Int): ByteArray { //Make npc's invisible
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.value)
            lew.write(0)
            lew.writeInt(oid)
            return lew.getPacket()
        }

        /* 00 = /
         * 01 = You don't have enough in stock
         * 02 = You do not have enough mesos
         * 03 = Please check if your inventory is full or not
         * 05 = You don't have enough in stock
         * 06 = Due to an error, the trade did not happen
         * 07 = Due to an error, the trade did not happen
         * 08 = /
         * 0D = You need more items
         * 0E = CRASH; LENGTH NEEDS TO BE LONGER :O
         */
        fun shopTransaction(code: Byte): ByteArray {
            val lew = PacketLittleEndianWriter(3)
            lew.write(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.value)
            lew.write(code)
            return lew.getPacket()
        }

        fun spawnNpc(life: Npc): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_NPC.value)
            lew.writeInt(life.objectId)
            lew.writeInt(life.id)
            lew.writeShort(life.position.x)
            lew.writeShort(life.cy)
            if (life.f == 1) {
                lew.write(0)
            } else {
                lew.write(1)
            }
            lew.writeShort(life.fh ?: 0)
            lew.writeShort(life.rx0)
            lew.writeShort(life.rx1)
            lew.write(1)
            return lew.getPacket()
        }

        fun spawnNpcRequestController(life: Npc): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.write(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.value)
            lew.write(1)
            lew.writeInt(life.objectId)
            lew.writeInt(life.id)
            lew.writeShort(life.position.x)
            lew.writeShort(life.cy)
            if (life.f == 1) {
                lew.write(0)
            } else {
                lew.write(1)
            }
            lew.writeShort(life.fh!!)
            lew.writeShort(life.rx0)
            lew.writeShort(life.rx1)
            lew.write(1)
            return lew.getPacket()
        }
    }
}