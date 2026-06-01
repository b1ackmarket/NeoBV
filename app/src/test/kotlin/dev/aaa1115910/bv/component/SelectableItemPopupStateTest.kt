package dev.aaa1115910.bv.component

import kotlin.test.Test
import kotlin.test.assertEquals

class SelectableItemPopupStateTest {
    @Test
    fun `popup uses upstream four column layout`() {
        assertEquals(4, SelectableItemPopupColumns)
        assertEquals(0.68f, SelectableItemPopupWidthFraction)
    }

    @Test
    fun `inline filter chips keep default width`() {
        assertEquals(0, FilterChipDefaults.MinWidth.value.toInt())
        assertEquals(184, FilterChipDefaults.MaxWidth.value.toInt())
        assertEquals(224, FilterChipDefaults.SelectorMaxWidth.value.toInt())
    }
}
