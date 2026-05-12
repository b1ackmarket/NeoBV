package dev.aaa1115910.bv.component

import kotlin.test.Test
import kotlin.test.assertEquals

class TopNavStateTest {
    @Test
    fun `clicking a nav item selects that item before invoking click action`() {
        val result = resolveTopNavClick(
            items = SearchTypeTopNavItem.entries,
            clicked = SearchTypeTopNavItem.MediaFt
        )

        assertEquals(SearchTypeTopNavItem.MediaFt, result.selectedItem)
        assertEquals(SearchTypeTopNavItem.MediaFt.ordinal, result.selectedIndex)
    }
}
