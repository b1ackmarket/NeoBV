package dev.aaa1115910.bv.component.controllers

import kotlin.test.Test
import kotlin.test.assertEquals

class MenuControllerStateTest {
    @Test
    fun `player menu defaults to play speed when opened`() {
        val state = defaultPlayerMenuNavState()

        assertEquals(VideoPlayerMenuNavItem.PlaySpeed, state.selectedNavItem)
        assertEquals(VideoPlayerMenuNavItem.PlaySpeed, state.focusedNavItem)
        assertEquals(MenuFocusState.MenuNav, state.focusState)
    }
}
