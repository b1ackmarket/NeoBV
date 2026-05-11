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
    fun `rooms below first row explicitly route up to previous row item`() {
        val columns = 4

        assertFalse(shouldRouteLiveRoomUpToCategory(index = 4, columns = columns))
        assertTrue(shouldRouteLiveRoomUpToPreviousRow(index = 4, columns = columns))
        assertTrue(shouldRouteLiveRoomUpToPreviousRow(index = 7, columns = columns))
    }

    @Test
    fun `invalid grid column count never steals default focus search`() {
        assertFalse(shouldRouteLiveRoomUpToCategory(index = 0, columns = 0))
        assertFalse(shouldRouteLiveRoomUpToPreviousRow(index = 4, columns = 0))
    }
}
