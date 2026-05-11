package dev.aaa1115910.bv.component.controllers.playermenu

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerStatsMenuStateTest {
    @Test
    fun `stats menu selected index follows current enabled state`() {
        assertEquals(0, resolvePlayerStatsMenuSelectedIndex(showPlayerStats = false))
        assertEquals(1, resolvePlayerStatsMenuSelectedIndex(showPlayerStats = true))
    }
}
