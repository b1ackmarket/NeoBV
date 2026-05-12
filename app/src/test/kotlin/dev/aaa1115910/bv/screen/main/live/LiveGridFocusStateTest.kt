package dev.aaa1115910.bv.screen.main.live

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiveGridFocusStateTest {
    @Test
    fun `only first row routes up to category row`() {
        val columns = 4

        assertTrue(shouldRouteLiveRoomUpToCategory(index = 0, columns = columns))
        assertTrue(shouldRouteLiveRoomUpToCategory(index = 3, columns = columns))
        assertFalse(shouldRouteLiveRoomUpToCategory(index = 4, columns = columns))
        assertFalse(shouldRouteLiveRoomUpToCategory(index = 7, columns = columns))
    }

    @Test
    fun `rooms below first row keep default lazy grid focus search`() {
        val columns = 4

        assertFalse(shouldRouteLiveRoomUpToCategory(index = 4, columns = columns))
        assertFalse(shouldRouteLiveRoomUpToPreviousRow(index = 4, columns = columns))
        assertFalse(shouldRouteLiveRoomUpToPreviousRow(index = 7, columns = columns))
    }

    @Test
    fun `invalid grid column count never steals default focus search`() {
        assertFalse(shouldRouteLiveRoomUpToCategory(index = 0, columns = 0))
        assertFalse(shouldRouteLiveRoomUpToPreviousRow(index = 4, columns = 0))
    }

    @Test
    fun `live grid requests next page near the end of current rooms`() {
        assertFalse(shouldLoadMoreLiveRooms(focusedIndex = 20, roomCount = 30, preloadThreshold = 6))
        assertTrue(shouldLoadMoreLiveRooms(focusedIndex = 24, roomCount = 30, preloadThreshold = 6))
        assertFalse(shouldLoadMoreLiveRooms(focusedIndex = -1, roomCount = 30, preloadThreshold = 6))
    }

    @Test
    fun `menu key only returns to categories when room grid has focus`() {
        assertTrue(shouldHandleLiveMenuKey(isRoomGridFocused = true))
        assertFalse(shouldHandleLiveMenuKey(isRoomGridFocused = false))
    }

    @Test
    fun `down from category after menu targets first room`() {
        assertTrue(shouldResetLiveRoomGridOnMenu(isRoomGridFocused = true))
        assertFalse(shouldResetLiveRoomGridOnMenu(isRoomGridFocused = false))
        assertTrue(targetLiveRoomIndexAfterCategoryDown(roomCount = 30) == 0)
        assertTrue(targetLiveRoomIndexAfterCategoryDown(roomCount = 0) == null)
    }
}
