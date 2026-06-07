package dev.aaa1115910.bv.entity.live

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveDefaultQualityTest {
    @Test
    fun `live default quality exposes all known qn options from low to high clarity`() {
        assertEquals(
            listOf(
                "流畅",
                "高清",
                "超清",
                "蓝光",
                "原画",
                "2K",
                "4K",
                "原画真彩",
                "杜比"
            ),
            LiveDefaultQuality.entries.map { it.displayName }
        )
        assertEquals(
            listOf(80, 150, 250, 400, 10000, 15000, 20000, 25000, 30000),
            LiveDefaultQuality.entries.map { it.qn }
        )
    }

    @Test
    fun `unknown saved qn falls back to original quality`() {
        assertEquals(LiveDefaultQuality.Original, LiveDefaultQuality.fromQn(-1))
    }
}
