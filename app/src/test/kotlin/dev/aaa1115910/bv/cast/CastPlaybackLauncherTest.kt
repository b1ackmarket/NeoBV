package dev.aaa1115910.bv.cast

import kotlin.test.Test
import kotlin.test.assertEquals

class CastPlaybackLauncherTest {
    @Test
    fun `converts cast seek seconds to player milliseconds`() {
        assertEquals(0, null.toPlayedMillis())
        assertEquals(0, (-1).toPlayedMillis())
        assertEquals(71_000, 71.toPlayedMillis())
    }
}
