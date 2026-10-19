package app.screenclock.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockLayoutTest {
    @Test
    fun layoutChoicesRoundTripAndUnknownValuesUseExistingLayout() {
        for (choice in DatePosition.entries) {
            assertEquals(choice, DatePosition.fromStored(choice.name))
        }
        for (size in DigitSize.entries) {
            assertEquals(size, DigitSize.fromStored(size.name))
        }
        assertEquals(DatePosition.Above, DatePosition.fromStored(null))
        assertEquals(DatePosition.Above, DatePosition.fromStored("invalid"))
        assertEquals(DigitSize.Regular, DigitSize.fromStored(null))
        assertEquals(DigitSize.Regular, DigitSize.fromStored("invalid"))
    }

    @Test
    fun digitSizesIncreaseWithoutExcessiveOverflow() {
        assertTrue(DigitSize.Compact.factor < DigitSize.Regular.factor)
        assertTrue(DigitSize.Regular.factor < DigitSize.Large.factor)
        assertTrue(DigitSize.Large.factor <= 1.05f)
    }
}
