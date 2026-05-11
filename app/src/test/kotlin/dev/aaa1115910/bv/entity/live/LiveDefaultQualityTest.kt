package dev.aaa1115910.bv.entity.live

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveDefaultQualityTest {
    @Test
    fun `live default quality exposes web style options in settings order`() {
        assertEquals(
            listOf("1080P原画", "1080P高码率", "1080P蓝光", "720P超清"),
            LiveDefaultQuality.entries.map { it.displayName }
        )
        assertEquals(
            listOf(10000, 25000, 400, 250),
            LiveDefaultQuality.entries.map { it.qn }
        )
    }

    @Test
    fun `unknown saved qn falls back to original quality`() {
        assertEquals(LiveDefaultQuality.Original, LiveDefaultQuality.fromQn(-1))
    }
}
