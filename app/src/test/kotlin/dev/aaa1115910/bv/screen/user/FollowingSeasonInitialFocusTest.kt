package dev.aaa1115910.bv.screen.user

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowingSeasonInitialFocusTest {
    @Test
    fun `following season requests initial grid focus after items load`() {
        assertTrue(shouldRequestFollowingSeasonInitialFocus(itemCount = 1))
    }

    @Test
    fun `following season does not request initial grid focus for empty results`() {
        assertFalse(shouldRequestFollowingSeasonInitialFocus(itemCount = 0))
    }
}
