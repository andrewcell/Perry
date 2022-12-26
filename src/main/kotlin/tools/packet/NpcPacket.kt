package tools.packet

import client.Client
import constants.ItemConstants
import net.SendPacketOpcode
import server.ItemInformationProvider
import server.ShopItem
import server.life.Npc
import server.maps.PlayerNPCs
import tools.HexTool
import tools.PacketCreator
import tools.PacketCreator.Companion.packetWriter
import tools.data.output.PacketLittleEndianWriter

class NpcPacket {
    companion object {
        fun getNpcShop(c: Client, sid: Int, items: List<ShopItem>) = packetWriter(SendPacketOpcode.OPEN_NPC_SHOP) {
            int(sid)
            //short(items.size() - 27); // 0x26
            short(items.size) // 0x26
            for ((buyable, itemId, price) in items) {
                /*if(ItemConstants.isRechargable(item.getItemId())) {
                continue;
                }*/
                int(itemId)
                int(price)
                if (!ItemConstants.isRechargeable(itemId)) {
                    short(buyable.toInt())
                } else {
                    //short(0);
                    int(0)
                    short(0)
                    short(PacketCreator.doubleToShortBits(ItemInformationProvider.getPrice(itemId)))
                    short(ItemInformationProvider.getSlotMax(c, itemId).toInt())
                }
            }
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
        fun getNpcTalk(npc: Int, msgType: Byte, talk: String, endBytes: String) = packetWriter(SendPacketOpcode.NPC_TALK) {
            byte(4) // ?
            int(npc)
            byte(msgType)
            gameASCIIString(talk)
            byte(HexTool.getByteArrayFromHexString(endBytes))
        }

        fun getNpcTalkNum(npc: Int, talk: String, def: Int, min: Int, max: Int) = packetWriter(SendPacketOpcode.NPC_TALK) {
            byte(4) // ?
            int(npc)
            byte(3)
            //byte(0); //speaker
            gameASCIIString(talk)
            int(def)
            int(min)
            int(max)
            int(0)
        }

        fun getNpcTalkStyle(npc: Int, talk: String, styles: Array<Int>) = packetWriter(SendPacketOpcode.NPC_TALK) {
            byte(SendPacketOpcode.NPC_TALK.value)
            byte(4) // ?
            int(npc)
            byte(5)
            //byte(0); //speaker
            gameASCIIString(talk)
            byte(styles.size)
            for (i in styles.indices) {
                int(styles[i])
            }
        }

        fun getNpcTalkText(npc: Int, talk: String, def: String) = packetWriter(SendPacketOpcode.NPC_TALK) {
            byte(4) // Doesn't matter
            int(npc)
            byte(2)
            //byte(0); //speaker
            gameASCIIString(talk)
            gameASCIIString(def) //:D
            int(0)
        }

        fun getPlayerNpc(npc: PlayerNPCs) = packetWriter(SendPacketOpcode.IMITATED_NPC_RESULT) {
            byte(0x01)
            int(npc.npcId)
            gameASCIIString(npc.name)
            byte(0) // direction
            byte(npc.skin)
            int(npc.face)
            byte(0)
            int(npc.hair)
            val equip = npc.equips
            val myEquip: MutableMap<Byte, Int?> = mutableMapOf()
            for (position in equip.keys) {
                var pos = (position * -1).toByte()
                if (pos > 100) {
                    pos = (pos - 100).toByte()
                    myEquip[pos] = equip[position]
                } else {
                    if (myEquip[pos] == null) {
                        myEquip[pos] = equip[position]
                    }
                }
            }
            for ((key, value) in myEquip) {
                byte(key)
                int(value ?: 0)
            }
            short(-1)
            val cWeapon = equip[(-111).toByte()]
            if (cWeapon != null) {
                int(cWeapon)
            } else {
                int(0)
            }
            for (i in 0..11) {
                byte(0)
            }
        }

        fun removeNpc(oid: Int): ByteArray = packetWriter(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER) { //Make npc's invisible
            byte(0)
            int(oid)
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
        fun shopTransaction(code: Byte) = packetWriter(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION) {
            byte(code)
        }

        fun spawnNpc(life: Npc) = packetWriter(SendPacketOpcode.SPAWN_NPC) {
            int(life.objectId)
            int(life.id)
            short(life.position.x)
            short(life.cy)
            if (life.f == 1) {
                byte(0)
            } else {
                byte(1)
            }
            short(life.fh ?: 0)
            short(life.rx0)
            short(life.rx1)
            byte(1)
        }

        fun spawnNpcRequestController(life: Npc) = packetWriter(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER) {
            byte(1)
            int(life.objectId)
            int(life.id)
            short(life.position.x)
            short(life.cy)
            if (life.f == 1) {
                byte(0)
            } else {
                byte(1)
            }
            short(life.fh!!)
            short(life.rx0)
            short(life.rx1)
            byte(1)
        }
    }
}