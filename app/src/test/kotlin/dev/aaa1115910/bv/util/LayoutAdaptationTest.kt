package dev.aaa1115910.bv.util

import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutAdaptationTest {
    @Test
    fun `default density is clamped for high resolution phones`() {
        assertEquals(2.2f, resolveDefaultDensity(widthPx = 3200, heightPx = 1440))
    }

    @Test
    fun `default density stays near tv scale for 1080p landscape`() {
        assertEquals(2.0f, resolveDefaultDensity(widthPx = 1920, heightPx = 1080))
    }

    @Test
    fun `adaptive grid min width follows card family`() {
        assertEquals(220, resolveAdaptiveGridMinCellWidth(defaultColumns = 4))
        assertEquals(150, resolveAdaptiveGridMinCellWidth(defaultColumns = 6))
        assertEquals(250, resolveAdaptiveGridMinCellWidth(defaultColumns = 3))
    }
}
