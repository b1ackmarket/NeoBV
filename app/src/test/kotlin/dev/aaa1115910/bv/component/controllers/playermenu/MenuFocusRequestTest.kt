package dev.aaa1115910.bv.component.controllers.playermenu

import dev.aaa1115910.bv.component.controllers.MenuFocusState
import dev.aaa1115910.bv.component.controllers.VideoPlayerPictureMenuItem
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

    @Test
    fun `touching parent menu item selects it and opens tertiary items`() {
        val result = resolveParentMenuTouch(
            current = VideoPlayerPictureMenuItem.Resolution,
            touched = VideoPlayerPictureMenuItem.Audio
        )

        assertEquals(VideoPlayerPictureMenuItem.Audio, result.selectedItem)
        assertEquals(MenuFocusState.Items, result.focusState)
    }
}
