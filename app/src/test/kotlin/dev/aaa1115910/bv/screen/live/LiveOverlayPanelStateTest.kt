package dev.aaa1115910.bv.screen.live

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveOverlayPanelStateTest {
    @Test
    fun `menu key opens right menu`() {
        assertEquals(LiveOverlayPanel.RightMenu, openLiveRightMenu())
    }

    @Test
    fun `menu key closes right menu when it is already open`() {
        assertEquals(
            LiveOverlayPanel.None,
            toggleLiveRightMenu(LiveOverlayPanel.RightMenu)
        )
    }

    @Test
    fun `menu key opens right menu from no overlay`() {
        assertEquals(
            LiveOverlayPanel.RightMenu,
            toggleLiveRightMenu(LiveOverlayPanel.None)
        )
    }

    @Test
    fun `down key opens bottom menu`() {
        assertEquals(LiveOverlayPanel.BottomMenu, openLiveBottomMenu())
    }
}
