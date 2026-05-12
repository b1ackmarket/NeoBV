package dev.aaa1115910.bv.entity.live

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveDefaultQualityTest {
    @Test
    fun `live default quality exposes all known qn options from low to high clarity`() {
        assertEquals(
            listOf(
                "360P流畅",
                "480P高清",
                "720P超清",
                "1080P蓝光",
                "1080P原画",
                "1080P高码率",
                "2K原画",
                "4K原画",
                "杜比视界"
            ),
            LiveDefaultQuality.entries.map { it.displayName }
        )
        assertEquals(
            listOf(80, 150, 250, 400, 10000, 25000, 15000, 20000, 30000),
            LiveDefaultQuality.entries.map { it.qn }
        )
    }

    @Test
    fun `unknown saved qn falls back to original quality`() {
        assertEquals(LiveDefaultQuality.Original, LiveDefaultQuality.fromQn(-1))
    }
}
