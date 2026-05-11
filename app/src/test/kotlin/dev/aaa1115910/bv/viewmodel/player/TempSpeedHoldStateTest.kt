package dev.aaa1115910.bv.viewmodel.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TempSpeedHoldStateTest {
    @Test
    fun `long press enters temporary 2x and key up restores previous speed`() {
        val state = TempSpeedHoldState()

        state.onLongPressTriggered(currentSpeed = 1.25f)

        assertTrue(state.isHoldingSpeed)
        assertEquals(1.25f, state.originalSpeed)
        assertEquals(2.0f, state.temporarySpeed)

        val restored = state.onKeyReleased()

        assertEquals(1.25f, restored)
        assertFalse(state.isHoldingSpeed)
    }

    @Test
    fun `second release after reset keeps current speed unchanged`() {
        val state = TempSpeedHoldState()

        state.onLongPressTriggered(currentSpeed = 1.5f)
        state.onKeyReleased()
        val restored = state.onKeyReleased()

        assertEquals(1.5f, restored)
        assertFalse(state.isHoldingSpeed)
    }
}
