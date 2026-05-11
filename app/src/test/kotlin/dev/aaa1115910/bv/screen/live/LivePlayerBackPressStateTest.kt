package dev.aaa1115910.bv.screen.live

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LivePlayerBackPressStateTest {
    @Test
    fun `back closes active overlay before exit prompt`() {
        val result = handleLiveBackPress(
            activeOverlay = LiveOverlayPanel.RightMenu,
            lastBackPressedAt = 0L,
            now = 1_000L
        )

        assertEquals(LiveOverlayPanel.None, result.activeOverlay)
        assertEquals(0L, result.lastBackPressedAt)
        assertEquals(null, result.statusText)
        assertFalse(result.shouldExit)
    }

    @Test
    fun `back primes exit prompt when no overlay is visible`() {
        val result = handleLiveBackPress(
            activeOverlay = LiveOverlayPanel.None,
            lastBackPressedAt = 0L,
            now = 1_000L
        )

        assertEquals(LiveOverlayPanel.None, result.activeOverlay)
        assertEquals(1_000L, result.lastBackPressedAt)
        assertEquals("再次按下返回键退出播放", result.statusText)
        assertFalse(result.shouldExit)
    }

    @Test
    fun `second back within timeout exits playback when no overlay is visible`() {
        val result = handleLiveBackPress(
            activeOverlay = LiveOverlayPanel.None,
            lastBackPressedAt = 1_000L,
            now = 2_500L
        )

        assertTrue(result.shouldExit)
        assertEquals(null, result.statusText)
    }
}
