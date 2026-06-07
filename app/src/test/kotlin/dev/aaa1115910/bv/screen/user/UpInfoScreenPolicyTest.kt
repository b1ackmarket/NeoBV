package dev.aaa1115910.bv.screen.user

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpInfoScreenPolicyTest {
    @Test
    fun `up follow button requires login and valid mid`() {
        assertTrue(shouldShowUpFollowButton(isLogin = true, upMid = 1L))
        assertFalse(shouldShowUpFollowButton(isLogin = false, upMid = 1L))
        assertFalse(shouldShowUpFollowButton(isLogin = true, upMid = 0L))
    }
}
