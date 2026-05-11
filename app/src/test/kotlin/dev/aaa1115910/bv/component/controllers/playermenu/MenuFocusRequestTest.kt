package dev.aaa1115910.bv.component.controllers.playermenu

import kotlin.test.Test
import kotlin.test.assertEquals

class MenuFocusRequestTest {
    @Test
    fun `returning from tertiary menu keeps the previously focused secondary item`() {
        assertEquals(
            3,
            resolveParentMenuFocusIndex(selectedIndex = 3, itemCount = 6)
        )
    }

    @Test
    fun `invalid secondary focus falls back to first item`() {
        assertEquals(
            0,
            resolveParentMenuFocusIndex(selectedIndex = 9, itemCount = 3)
        )
    }
}
