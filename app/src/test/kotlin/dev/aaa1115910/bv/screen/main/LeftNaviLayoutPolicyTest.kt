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
    fun `pgc icon uses adjusted visual size to match other nav icons`() {
        assertEquals(22, LeftNaviLayoutPolicy.iconSizeDp(LeftNaviItem.PGC))
        assertEquals(24, LeftNaviLayoutPolicy.iconSizeDp(LeftNaviItem.Home))
        assertEquals(24, LeftNaviLayoutPolicy.iconSizeDp(LeftNaviItem.Live))
    }
}
