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
            lew.byte(SendPacketOpcode.OPEN_NPC_SHOP.value)
            lew.int(sid)
            //lew.short(items.size() - 27); // 0x26
            lew.short(items.size) // 0x26
            for ((buyable, itemId, price) in items) {
                /*if(ItemConstants.isRechargable(item.getItemId())) {
                continue;
            }*/
                lew.int(itemId)
                lew.int(price)
                if (!ItemConstants.isRechargeable(itemId)) {
                    lew.short(buyable.toInt())
                } else {
                    //lew.short(0);
                    lew.int(0)
                    lew.short(0)
                    lew.short(PacketCreator.doubleToShortBits(ItemInformationProvider.getPrice(itemId)))
                    lew.short(ItemInformationProvider.getSlotMax(c, itemId).toInt())
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
            lew.byte(SendPacketOpcode.NPC_TALK.value)
            lew.byte(4) // ?
            lew.int(npc)
            lew.byte(msgType)
            lew.gameASCIIString(talk)
            lew.byte(HexTool.getByteArrayFromHexString(endBytes))
            return lew.getPacket()
        }

        fun getNpcTalkNum(npc: Int, talk: String, def: Int, min: Int, max: Int): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.NPC_TALK.value)
            lew.byte(4) // ?
            lew.int(npc)
            lew.byte(3)
            //lew.byte(0); //speaker
            lew.gameASCIIString(talk)
            lew.int(def)
            lew.int(min)
            lew.int(max)
            lew.int(0)
            return lew.getPacket()
        }

        fun getNpcTalkStyle(npc: Int, talk: String, styles: Array<Int>): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.NPC_TALK.value)
            lew.byte(4) // ?
            lew.int(npc)
            lew.byte(5)
            //lew.byte(0); //speaker
            lew.gameASCIIString(talk)
            lew.byte(styles.size)
            for (i in styles.indices) {
                lew.int(styles[i])
            }
            return lew.getPacket()
        }

        fun getNpcTalkText(npc: Int, talk: String, def: String): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.NPC_TALK.value)
            lew.byte(4) // Doesn't matter
            lew.int(npc)
            lew.byte(2)
            //lew.byte(0); //speaker
            lew.gameASCIIString(talk)
            lew.gameASCIIString(def) //:D
            lew.int(0)
            return lew.getPacket()
        }

        fun removeNpc(oid: Int): ByteArray { //Make npc's invisible
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.value)
            lew.byte(0)
            lew.int(oid)
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
            lew.byte(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.value)
            lew.byte(code)
            return lew.getPacket()
        }

        fun spawnNpc(life: Npc): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SPAWN_NPC.value)
            lew.int(life.objectId)
            lew.int(life.id)
            lew.short(life.position.x)
            lew.short(life.cy)
            if (life.f == 1) {
                lew.byte(0)
            } else {
                lew.byte(1)
            }
            lew.short(life.fh ?: 0)
            lew.short(life.rx0)
            lew.short(life.rx1)
            lew.byte(1)
            return lew.getPacket()
        }

        fun spawnNpcRequestController(life: Npc): ByteArray {
            val lew = PacketLittleEndianWriter()
            lew.byte(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.value)
            lew.byte(1)
            lew.int(life.objectId)
            lew.int(life.id)
            lew.short(life.position.x)
            lew.short(life.cy)
            if (life.f == 1) {
                lew.byte(0)
            } else {
                lew.byte(1)
            }
            lew.short(life.fh!!)
            lew.short(life.rx0)
            lew.short(life.rx1)
            lew.byte(1)
            return lew.getPacket()
        }
    }
}