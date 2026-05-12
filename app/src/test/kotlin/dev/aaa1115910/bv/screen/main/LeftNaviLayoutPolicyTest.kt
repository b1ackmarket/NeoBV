package dev.aaa1115910.bv.screen.main

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LeftNaviLayoutPolicyTest {
    @Test
    fun `compact landscape phone should enable scrollable left rail`() {
        assertTrue(
            LeftNaviLayoutPolicy.shouldUseScrollableRail(screenHeightDp = 411)
        )
    }

    @Test
    fun `tv height should keep fixed left rail`() {
        assertFalse(
            LeftNaviLayoutPolicy.shouldUseScrollableRail(screenHeightDp = 720)
        )
    }

    @Test
    fun `nav icons use unified visual size from redesigned asset set`() {
        assertEquals(24, LeftNaviLayoutPolicy.iconSizeDp(LeftNaviItem.PGC))
        assertEquals(24, LeftNaviLayoutPolicy.iconSizeDp(LeftNaviItem.Home))
        assertEquals(24, LeftNaviLayoutPolicy.iconSizeDp(LeftNaviItem.Live))
    }
}
