
import client.inventory.InventoryType
import constants.ItemConstants
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ItemConstantsTest {

    @Test
    fun `getFlagByInt returns correct flag for known type`() {
        assertEquals(ItemConstants.PET_COME, ItemConstants.getFlagByInt(128))
        assertEquals(ItemConstants.UNKNOWN_SKILL, ItemConstants.getFlagByInt(256))
    }

    @Test
    fun `getFlagByInt returns zero for unknown type`() {
        assertEquals(0, ItemConstants.getFlagByInt(999))
    }

    @Test
    fun `isThrowingStar returns true for throwing star item id`() {
        assertTrue(ItemConstants.isThrowingStar(2070000))
    }

    @Test
    fun `isThrowingStar returns false for non-throwing star item id`() {
        assertFalse(ItemConstants.isThrowingStar(2080000))
    }

    @Test
    fun `isBullet returns true for bullet item id`() {
        assertTrue(ItemConstants.isBullet(2330000))
    }

    @Test
    fun `isBullet returns false for non-bullet item id`() {
        assertFalse(ItemConstants.isBullet(2340000))
    }

    @Test
    fun `isRechargeable returns true for rechargeable item id`() {
        assertTrue(ItemConstants.isRechargeable(2070000))
        assertTrue(ItemConstants.isRechargeable(2330000))
    }

    @Test
    fun `isRechargeable returns false for non-rechargeable item id`() {
        assertFalse(ItemConstants.isRechargeable(2080000))
    }

    @Test
    fun `isArrowForCrossBow returns true for crossbow arrow item id`() {
        assertTrue(ItemConstants.isArrowForCrossBow(2061000))
    }

    @Test
    fun `isArrowForCrossBow returns false for non-crossbow arrow item id`() {
        assertFalse(ItemConstants.isArrowForCrossBow(2060000))
    }

    @Test
    fun `isArrowForBow returns true for bow arrow item id`() {
        assertTrue(ItemConstants.isArrowForBow(2060000))
    }

    @Test
    fun `isArrowForBow returns false for non-bow arrow item id`() {
        assertFalse(ItemConstants.isArrowForBow(2061000))
    }

    @Test
    fun `isPet returns true for pet item id`() {
        assertTrue(ItemConstants.isPet(5000000))
    }

    @Test
    fun `isPet returns false for non-pet item id`() {
        assertFalse(ItemConstants.isPet(6000000))
    }

    @Test
    fun `getInventoryType returns correct inventory type for known item id`() {
        assertEquals(InventoryType.EQUIP, ItemConstants.getInventoryType(1000000))
    }

    @Test
    fun `getInventoryType returns UNDEFINED for unknown item id`() {
        assertEquals(InventoryType.UNDEFINED, ItemConstants.getInventoryType(6000000))
    }
}