package dev.aaa1115910.bv.screen.settings.content

import kotlin.test.Test
import kotlin.test.assertEquals

class UIDensityDialogStateTest {
    @Test
    fun `density steps use current displayed value while stored preference lags`() {
        val state = UIDensityDialogState(initialDensity = 2.0f)

        assertEquals(1.9f, state.step(direction = -1))
        assertEquals(1.8f, state.step(direction = -1))
        assertEquals(1.7f, state.step(direction = -1))
    }

    @Test
    fun `density steps clamp to allowed range`() {
        val lower = UIDensityDialogState(initialDensity = 0.5f)
        val upper = UIDensityDialogState(initialDensity = 5.0f)

        assertEquals(0.5f, lower.step(direction = -1))
        assertEquals(5.0f, upper.step(direction = 1))
    }

    @Test
    fun `density state resets from stored preference when dialog opens`() {
        val state = UIDensityDialogState(initialDensity = 1.0f)
        state.step(direction = 1)

        state.reset(2.3f)

        assertEquals(2.3f, state.displayDensity)
    }
}
