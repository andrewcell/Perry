package tools

import client.BuffStat
import client.Character
import client.Disease
import client.KeyBinding
import client.inventory.Item
import io.mockk.*
import net.SendPacketOpcode
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import server.DueyPackages
import server.MiniGame
import server.PlayerShop
import server.maps.GameMap
import tools.data.output.PacketLittleEndianWriter
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PacketCreatorTest {

    @BeforeAll
    fun setup() {
        // Mock SendPacketOpcode enum values
        mockkObject(SendPacketOpcode)
        every { SendPacketOpcode.KEYMAP.value } returns 0x12
        every { SendPacketOpcode.BLOCKED_MAP.value } returns 0x20
        every { SendPacketOpcode.CONTI_MOVE.value } returns 0x21
        every { SendPacketOpcode.BRIDLE_MOB_CATCH_FAIL.value } returns 0x22
        every { SendPacketOpcode.ADMIN_RESULT.value } returns 0x23
        every { SendPacketOpcode.DISABLE_UI.value } returns 0x24
        every { SendPacketOpcode.SCRIPT_PROGRESS_MESSAGE.value } returns 0x25
        every { SendPacketOpcode.STAT_CHANGED.value } returns 0x26
        every { SendPacketOpcode.CLAIM_STATUS_CHANGED.value } returns 0x27
        every { SendPacketOpcode.ENABLE_TV.value } returns 0x28
        every { SendPacketOpcode.CLOCK.value } returns 0x29
        every { SendPacketOpcode.RELOG_RESPONSE.value } returns 0x2A
        every { SendPacketOpcode.SET_FIELD.value } returns 0x2B
        every { SendPacketOpcode.TALK_GUIDE.value } returns 0x2C
        every { SendPacketOpcode.LEFT_KNOCK_BACK.value } returns 0x2D
        every { SendPacketOpcode.LOCK_UI.value } returns 0x2E
        every { SendPacketOpcode.OPEN_UI.value } returns 0x2F
        every { SendPacketOpcode.ENTRUSTED_SHOP_CHECK_RESULT.value } returns 0x30
        every { SendPacketOpcode.REMOVE_TV.value } returns 0x31
        every { SendPacketOpcode.SUE_CHARACTER_RESULT.value } returns 0x32
        every { SendPacketOpcode.FORCED_STAT_RESET.value } returns 0x33
        every { SendPacketOpcode.PARCEL.value } returns 0x34
        every { SendPacketOpcode.PLAYER_HINT.value } returns 0x35
        every { SendPacketOpcode.TRADE_MONEY_LIMIT.value } returns 0x36
        every { SendPacketOpcode.SEND_TV.value } returns 0x37
        every { SendPacketOpcode.SET_WEEK_EVENT_MESSAGE.value } returns 0x38
        every { SendPacketOpcode.SHOW_FOREIGN_EFFECT.value } returns 0x39
        every { SendPacketOpcode.FIELD_EFFECT.value } returns 0x3A
        every { SendPacketOpcode.SHOW_CHAIR.value } returns 0x3B
        every { SendPacketOpcode.MEMO_RESULT.value } returns 0x3C
        every { SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.value } returns 0x3D
        every { SendPacketOpcode.SHOW_STATUS_INFO.value } returns 0x3E
        every { SendPacketOpcode.UPDATE_CHAR_BOX.value } returns 0x3F
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `test packetWriter with opcode`() {
        val result = PacketCreator.packetWriter(SendPacketOpcode.KEYMAP, 10) {
            byte(1)
            int(100)
        }

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test packetWriter without opcode`() {
        val result = PacketCreator.packetWriter(null, 10) {
            byte(1)
            int(100)
        }

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test addAnnounceBox with null game`() {
        val result = PacketCreator.addAnnounceBox(null, 1, 0, 0, 0, 0)

        assertNotNull(result)
    }

    @Test
    fun `test addAnnounceBox with game`() {
        val mockGame = mockk<MiniGame>(relaxed = true)
        every { mockGame.objectId } returns 12345
        every { mockGame.description } returns "Test Game"

        val result = PacketCreator.addAnnounceBox(mockGame, 1, 5, 2, 10, 1)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test addAnnounceBox with shop - null shop`() {
        val lew = PacketLittleEndianWriter()

        PacketCreator.addAnnounceBox(lew, null, 1)

        val result = lew.getPacket()
        assertEquals(0, result.size)
    }

    @Test
    fun `test addAnnounceBox with shop`() {
        val mockShop = mockk<PlayerShop>(relaxed = true)
        every { mockShop.objectId } returns 54321
        every { mockShop.description } returns "Test Shop"

        val lew = PacketLittleEndianWriter()
        PacketCreator.addAnnounceBox(lew, mockShop, 1)

        val result = lew.getPacket()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test blockedMessage`() {
        val result = PacketCreator.blockedMessage(1)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test boatPacket with true`() {
        val result = PacketCreator.boatPacket(true)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test boatPacket with false`() {
        val result = PacketCreator.boatPacket(false)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test catchMessage`() {
        val result = PacketCreator.catchMessage(1)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test customPacket with string`() {
        val result = PacketCreator.customPacket("0A0B0C")

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test customPacket with bytearray`() {
        val input = byteArrayOf(0x0A, 0x0B, 0x0C)
        val result = PacketCreator.customPacket(input)

        assertNotNull(result)
        assertEquals(input.size, result.size)
    }

    @Test
    fun `test disableMinimap`() {
        val result = PacketCreator.disableMinimap()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test disableUI enable true`() {
        val result = PacketCreator.disableUI(true)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test disableUI enable false`() {
        val result = PacketCreator.disableUI(false)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test doubleToShortBits`() {
        val result = PacketCreator.doubleToShortBits(123.456)

        assertNotNull(result)
    }

    @Test
    fun `test earnTitleMessage`() {
        val result = PacketCreator.earnTitleMessage("Test Message")

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test enableActions`() {
        val result = PacketCreator.enableActions()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test enableReport`() {
        val result = PacketCreator.enableReport()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test enableTV`() {
        val result = PacketCreator.enableTV()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test getClock`() {
        val result = PacketCreator.getClock(60)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test getClockTime`() {
        val result = PacketCreator.getClockTime(12, 30, 45)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test getGMEffect`() {
        val result = PacketCreator.getGMEffect(0x04, 0)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test getKeyMap with empty bindings`() {
        val result = PacketCreator.getKeyMap(emptyMap())

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test getKeyMap with bindings`() {
        val mockBinding = mockk<KeyBinding>(relaxed = true)
        every { mockBinding.type } returns 1
        every { mockBinding.action } returns 100

        val keybindings = mapOf(0 to mockBinding, 5 to mockBinding)
        val result = PacketCreator.getKeyMap(keybindings)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test getLongMaskD with empty list`() {
        val result = PacketCreator.getLongMaskD<Disease>(emptyList())

        assertEquals(0L, result)
    }

    @Test
    fun `test getLongMaskD with values`() {
        val mockDisease = mockk<Disease>(relaxed = true)
        every { mockDisease.value } returns 0x01L

        val statUps = listOf(mockDisease to 1, mockDisease to 2)
        val result = PacketCreator.getLongMaskD<Disease>(statUps)

        assertTrue(result > 0)
    }

    @Test
    fun `test getRelogResponse`() {
        val result = PacketCreator.getRelogResponse()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test getTime with -1`() {
        val result = PacketCreator.getTime(-1L)

        assertEquals(150842304000000000L, result)
    }

    @Test
    fun `test getTime with -2`() {
        val result = PacketCreator.getTime(-2L)

        assertEquals(PacketCreator.ZERO_TIME, result)
    }

    @Test
    fun `test getTime with -3`() {
        val result = PacketCreator.getTime(-3L)

        assertEquals(150841440000000000L, result)
    }

    @Test
    fun `test getTime with positive value`() {
        val timestamp = 1000000L
        val result = PacketCreator.getTime(timestamp)

        assertTrue(result > 0)
    }

    @Test
    fun `test guideHint`() {
        val result = PacketCreator.guideHint(123)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test leftKnockBack`() {
        val result = PacketCreator.leftKnockBack()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test lockUI enable true`() {
        val result = PacketCreator.lockUI(true)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test lockUI enable false`() {
        val result = PacketCreator.lockUI(false)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test openUI`() {
        val result = PacketCreator.openUI(0x01)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test remoteChannelChange`() {
        val result = PacketCreator.remoteChannelChange(1)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test removeTV`() {
        val result = PacketCreator.removeTV()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test reportResponse`() {
        val result = PacketCreator.reportResponse(0)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test resetForcedStats`() {
        val result = PacketCreator.resetForcedStats()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test retrieveFirstMessage`() {
        val result = PacketCreator.retrieveFirstMessage()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test sendDuey with operation not 8`() {
        val result = PacketCreator.sendDuey(5)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test sendDuey with operation 8 null packages`() {
        val result = PacketCreator.sendDuey(8, null)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test sendDuey with operation 8 and packages`() {
        val mockPackage = mockk<DueyPackages>(relaxed = true)
        every { mockPackage.packageId } returns 123
        every { mockPackage.sender } returns "TestSender"
        every { mockPackage.mesos } returns 1000
        every { mockPackage.sentTimeInMilliseconds() } returns 123456789L
        every { mockPackage.item } returns null

        val result = PacketCreator.sendDuey(8, listOf(mockPackage))

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test sendHint with width less than 1`() {
        val result = PacketCreator.sendHint("Test", 0, 3)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test sendHint with small hint`() {
        val result = PacketCreator.sendHint("Hi", 0, 3)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test sendHint with height less than 5`() {
        val result = PacketCreator.sendHint("Test", 100, 2)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test sendMesoLimit`() {
        val result = PacketCreator.sendMesoLimit()

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test sendYellowTip`() {
        val result = PacketCreator.sendYellowTip("Test tip")

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test longMask`() {
        val mockBuffStat = mockk<BuffStat>(relaxed = true)
        every { mockBuffStat.value } returns 0x01L

        val lew = PacketLittleEndianWriter()
        PacketCreator.longMask(lew, listOf(mockBuffStat to 1))

        val result = lew.getPacket()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test longMaskFromList with first buffs`() {
        val mockBuffStat = mockk<BuffStat>(relaxed = true)
        every { mockBuffStat.isFirst } returns true
        every { mockBuffStat.value } returns 0x01L

        val lew = PacketLittleEndianWriter()
        PacketCreator.longMaskFromList(lew, listOf(mockBuffStat))

        val result = lew.getPacket()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test longMaskFromList with second buffs`() {
        val mockBuffStat = mockk<BuffStat>(relaxed = true)
        every { mockBuffStat.isFirst } returns false
        every { mockBuffStat.value } returns 0x02L

        val lew = PacketLittleEndianWriter()
        PacketCreator.longMaskFromList(lew, listOf(mockBuffStat))

        val result = lew.getPacket()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test constants values`() {
        assertEquals(94354848000000000L, PacketCreator.ZERO_TIME)
        assertTrue(PacketCreator.EMPTY_STATUPDATE.isEmpty())
    }
}